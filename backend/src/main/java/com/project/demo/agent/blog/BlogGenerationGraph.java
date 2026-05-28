package com.project.demo.agent.blog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.agent.blog.agent.CodeAgent;
import com.project.demo.agent.blog.agent.ContentAgent;
import com.project.demo.agent.blog.agent.ImageAgent;
import com.project.demo.dto.BlogAgentSSEEvent;
import com.project.demo.dto.Outline;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * 博客生成子图 — 纯内容生成管道，无 checkpoint、无中断。
 *
 * <pre>
 *  START → designOutline → contentAgent → [codeAgent | imageAgent] → assembleBlog → END
 * </pre>
 *
 * 大纲只规划内容结构；正文先写完；然后代码和配图 Agent 并行运行，
 * 各自读取正文后自行判断在何处插入内容；最后拼装合并。
 */
@Slf4j
@Service
public class BlogGenerationGraph {

    private final ContentAgent contentAgent;
    private final CodeAgent codeAgent;
    private final ImageAgent imageAgent;
    private final BlogAgentEventManager eventManager;
    private final OpenAiChatModel jsonChatModel;
    private final ObjectMapper objectMapper;

    @Getter
    private CompiledGraph<BlogAgentState> graph;

    public BlogGenerationGraph(ContentAgent contentAgent,
                               CodeAgent codeAgent,
                               ImageAgent imageAgent,
                               BlogAgentEventManager eventManager,
                               OpenAiChatModel jsonChatModel,
                               ObjectMapper objectMapper) {
        this.contentAgent = contentAgent;
        this.codeAgent = codeAgent;
        this.imageAgent = imageAgent;
        this.eventManager = eventManager;
        this.jsonChatModel = jsonChatModel;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            this.graph = buildGraph().compile();
            log.info("BlogGenerationGraph 编译完成（新拓扑: outline → content → [code|image] → assemble）");
        } catch (GraphStateException e) {
            throw new RuntimeException("BlogGenerationGraph 编译失败", e);
        }
    }

    private StateGraph<BlogAgentState> buildGraph() throws GraphStateException {
        return new StateGraph<>(BlogAgentState.factory())
            .addNode("designOutline",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(designOutline(state)))
            .addNode("contentAgent",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(runContentAgent(state)))
            .addNode("codeAgent",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(runCodeAgent(state)))
            .addNode("imageAgent",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(runImageAgent(state)))
            .addNode("assembleBlog",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(assembleBlog(state)))

            // 新拓扑：outline → content → [code | image] → assemble → END
            .addEdge(START, "designOutline")
            .addEdge("designOutline", "contentAgent")
            .addEdge("contentAgent", "codeAgent")
            .addEdge("contentAgent", "imageAgent")
            .addEdge("codeAgent", "assembleBlog")
            .addEdge("imageAgent", "assembleBlog")
            .addEdge("assembleBlog", END);
    }

    // ═══════════════════════════════════════════════════════════════
    // 节点：大纲设计（只规划正文结构）
    // ═══════════════════════════════════════════════════════════════
    private Map<String, Object> designOutline(BlogAgentState state) {
        String topic = state.getTopic();
        String sessionId = state.getSessionId();
        String requirements = state.getRequirements();

        log.info("[DesignOutline] 设计大纲: topic={}", topic);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("outline")
            .message("正在设计博客大纲...")
            .build());

        String rawOutput = jsonChatModel.chat("""
            你是一个技术博客编辑。根据给定主题，设计一篇博客的大纲。

            ## 输出格式（严格遵守）
            必须输出一个 JSON 对象（不是数组！），包含 title 和 sections 字段：
            {
              "title": "博客标题",
              "sections": [
                {
                  "heading": "## 章节标题",
                  "keyPoints": ["要点1", "要点2"]
                }
              ]
            }

            ## 要求
            - 必须包含 "title" 字段，不能省略
            - 必须输出对象格式 {...}，不要输出数组格式 [{...}]
            - 大纲只规划正文内容和结构
            - 不需要标注代码提示或配图提示
            - 只输出 JSON，不要其他内容（不要 markdown 代码块）

            主题: %s
            %s
            """.formatted(topic,
                requirements != null && !requirements.isBlank()
                    ? "用户附加要求: " + requirements : ""));

        String outlineJson = stripJsonFences(rawOutput);
        Outline outline = OutlineHelper.fromJson(outlineJson);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("outline")
            .message("大纲设计完成: " + outline.getTitle())
            .data(outlineJson)
            .build());

        return Map.of(
            BlogAgentState.KEY_OUTLINE, outlineJson,
            BlogAgentState.KEY_BLOG_TITLE, outline.getTitle()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // 节点：写正文（完整博客）
    // ═══════════════════════════════════════════════════════════════
    private Map<String, Object> runContentAgent(BlogAgentState state) {
        String outlineJson = state.getOutline();
        String requirements = state.getRequirements();
        String sessionId = state.getSessionId();

        Outline outline = OutlineHelper.fromJson(outlineJson);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("content_progress")
            .message("正在撰写博客正文（共" + outline.getSections().size() + "个章节）...")
            .build());

        // 构建完整描述：大纲 + 用户附加要求
        StringBuilder prompt = new StringBuilder();
        prompt.append("博客标题: ").append(outline.getTitle()).append("\n\n");
        prompt.append("## 章节大纲\n\n");
        for (int i = 0; i < outline.getSections().size(); i++) {
            Outline.Section s = outline.getSections().get(i);
            prompt.append("### ").append(s.getHeading()).append("\n");
            prompt.append("要点: ").append(String.join(", ", s.getKeyPoints())).append("\n\n");
        }
        if (requirements != null && !requirements.isBlank()) {
            prompt.append("## 用户附加要求（必须遵守）\n").append(requirements).append("\n");
        }

        String content = contentAgent.writeContent(prompt.toString());

        log.info("[ContentAgent] 正文生成完成, length={}", content != null ? content.length() : 0);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("content_progress")
            .message("正文撰写完成（" + (content != null ? content.length() : 0) + "字）")
            .build());

        return Map.of(BlogAgentState.KEY_CONTENT_SECTIONS, content != null ? content : "");
    }

    // ═══════════════════════════════════════════════════════════════
    // 节点：代码 Agent（基于正文判断何处加代码）
    // ═══════════════════════════════════════════════════════════════
    private Map<String, Object> runCodeAgent(BlogAgentState state) {
        String content = state.getContentSections();
        String sessionId = state.getSessionId();

        if (content == null || content.isBlank()) {
            log.info("[CodeAgent] 正文为空，跳过");
            return Map.of(BlogAgentState.KEY_CODE_BLOCKS, "[]");
        }

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("code_progress")
            .message("正在分析正文，寻找合适的代码插入位置...")
            .build());

        log.info("[CodeAgent] 开始分析正文并生成代码, contentLength={}", content.length());
        String rawOutput = codeAgent.generateCode(content);

        // 清理并验证 JSON
        String json = stripJsonFences(rawOutput);
        try {
            objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("[CodeAgent] 输出不是有效 JSON，将使用原始输出: {}", e.getMessage());
            json = "[]";
        }

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("code_progress")
            .message("代码生成完成")
            .build());

        return Map.of(BlogAgentState.KEY_CODE_BLOCKS, json);
    }

    // ═══════════════════════════════════════════════════════════════
    // 节点：配图 Agent（调用 Qwen-Image-2.0 生成配图）
    // ═══════════════════════════════════════════════════════════════
    private Map<String, Object> runImageAgent(BlogAgentState state) {
        String content = state.getContentSections();
        String sessionId = state.getSessionId();

        if (content == null || content.isBlank()) {
            log.info("[ImageAgent] 正文为空，跳过");
            return Map.of(BlogAgentState.KEY_IMAGES, "[]");
        }

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("image_progress")
            .message("正在分析正文并生成配图...")
            .build());

        log.info("[ImageAgent] 开始分析正文并生成配图, contentLength={}", content.length());
        String rawOutput = imageAgent.generateImages(content);

        // 清理并验证 JSON
        String json = stripJsonFences(rawOutput);
        try {
            objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("[ImageAgent] 输出不是有效 JSON: {}", e.getMessage());
            json = "[]";
        }

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("image_progress")
            .message("配图生成完成")
            .build());

        return Map.of(BlogAgentState.KEY_IMAGES, json);
    }

    // ═══════════════════════════════════════════════════════════════
    // 节点：拼装博客
    // ═══════════════════════════════════════════════════════════════
    private Map<String, Object> assembleBlog(BlogAgentState state) {
        String blogTitle = state.getBlogTitle();
        String content = state.getContentSections();
        String codeBlocksJson = state.getCodeBlocks();
        String images = state.getImages();
        String sessionId = state.getSessionId();

        log.info("[AssembleBlog] 拼装博客: title={}, contentLength={}", blogTitle,
            content != null ? content.length() : 0);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("content_progress")
            .message("正在拼装博客...")
            .build());

        // 1. 基础内容
        StringBuilder full = new StringBuilder();
        full.append("# ").append(blogTitle).append("\n\n");

        // 2. 解析代码块并插入正文
        String merged = content;
        if (codeBlocksJson != null && !codeBlocksJson.isBlank()) {
            merged = insertCodeBlocks(merged, codeBlocksJson);
        }

        // 3. 解析配图并插入正文
        String imagesJson = state.getImages();
        if (imagesJson != null && !imagesJson.isBlank()) {
            merged = insertImages(merged, imagesJson);
        }

        full.append(merged);

        // 4. 清理未处理的标记
        String fullBlog = full.toString()
            .replaceAll("<!-- NEEDS_CODE: .*? -->", "")
            .replaceAll("<!-- NEEDS_IMAGE: .*? -->", "")
            .trim();

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("content_progress")
            .message("博客拼装完成")
            .build());

        return Map.of(BlogAgentState.KEY_FULL_BLOG, fullBlog);
    }

    // ═══════════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 将代码块插入到正文的指定章节后面。
     */
    private String insertCodeBlocks(String content, String codeBlocksJson) {
        try {
            List<Map<String, String>> blocks = objectMapper.readValue(
                codeBlocksJson, new TypeReference<List<Map<String, String>>>() {});
            if (blocks.isEmpty()) return content;

            String result = content;
            for (Map<String, String> block : blocks) {
                String after = block.get("after");
                String language = block.getOrDefault("language", "");
                String code = block.get("code");

                if (after == null || code == null || after.isBlank() || code.isBlank()) {
                    continue;
                }

                String codeBlock = "\n```" + language + "\n" + code + "\n```\n";

                // 在指定章节标题后插入代码块
                // 找到 "## 章节标题" 所在行的末尾（下一个 ## 或文件末尾之前）
                String escapedAfter = java.util.regex.Pattern.quote(after.trim());
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(" + escapedAfter + ".*?\\n)(?=## |$)", java.util.regex.Pattern.DOTALL);

                if (pattern.matcher(result).find()) {
                    result = pattern.matcher(result).replaceFirst("$1" + java.util.regex.Matcher.quoteReplacement(codeBlock));
                    log.info("[AssembleBlog] 在 '{}' 后插入代码块", after);
                } else {
                    // 找不到精确章节标题，追加到末尾
                    log.warn("[AssembleBlog] 未找到章节 '{}'，代码块追加到末尾", after);
                    result += "\n" + codeBlock;
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[AssembleBlog] 代码块插入失败", e);
            return content;
        }
    }

    /**
     * 将配图插入到正文的指定章节后面。
     */
    private String insertImages(String content, String imagesJson) {
        try {
            List<Map<String, String>> blocks = objectMapper.readValue(
                imagesJson, new TypeReference<List<Map<String, String>>>() {});
            if (blocks.isEmpty()) return content;

            String result = content;
            for (Map<String, String> block : blocks) {
                String after = block.get("after");
                String image = block.get("image");

                if (after == null || image == null || after.isBlank() || image.isBlank()) {
                    continue;
                }

                String escapedAfter = java.util.regex.Pattern.quote(after.trim());
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(" + escapedAfter + ".*?\\n)(?=## |$)", java.util.regex.Pattern.DOTALL);

                if (pattern.matcher(result).find()) {
                    result = pattern.matcher(result).replaceFirst(
                        "$1\n" + java.util.regex.Matcher.quoteReplacement(image) + "\n");
                    log.info("[AssembleBlog] 在 '{}' 后插入配图", after);
                } else {
                    log.warn("[AssembleBlog] 未找到章节 '{}'，配图追加到末尾", after);
                    result += "\n" + image + "\n";
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[AssembleBlog] 配图插入失败", e);
            return content;
        }
    }

    private String stripJsonFences(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            if (start < 0) start = 3;
            else start = start + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) trimmed = trimmed.substring(start, end);
            else trimmed = trimmed.substring(start);
        }
        return trimmed.trim();
    }
}
