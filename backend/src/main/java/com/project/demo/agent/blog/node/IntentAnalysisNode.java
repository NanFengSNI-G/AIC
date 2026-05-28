package com.project.demo.agent.blog.node;

import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.dto.BlogAgentSSEEvent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentAnalysisNode {

    private final IntentAgent intentAgent;
    private final BlogAgentEventManager eventManager;

    @AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "blogChatMemoryProvider"
    )
    public interface IntentAgent {

        @SystemMessage("""
        你是一个智能助手，具有两层能力：

        ## 1. 任务路由
        当用户想要写博客或搜索论坛，并且给出了足够的信息时，输出 JSON：
        {
          "action": "route",
          "intent": "write_blog" | "search_forum",
          "topic": "博客主题（write_blog 时提取，必须非空）",
          "keywords": "搜索关键词（search_forum 时提取）",
          "requirements": "用户的附加要求，如字数、是否要代码、配图、风格偏好等（无则填 null）"
        }

        ## 2. 信息不足时
        如果用户表达了写博客的意图但主题不明确时，请友好地反问用户想要什么主题、有什么要求。直接输出文本，不要输出 JSON。
        中文字数控制在 2-3 句以内。

        ## 3. 日常对话
        当用户进行普通对话、问候、闲聊、询问你是谁、或表达感谢时，直接给出简短的文本回复。
        中文字数控制在 2-3 句以内。
        不输出 JSON，直接输出文本。

        ## 规则
        - "帮我写一篇关于 XX 的博客"、"写一篇 XX" → write_blog（主题明确）
        - "写一篇有关"、"帮我写"、"写博客"（主题不明确）→ 反问用户要什么主题
        - "搜一下..."、"有没有..."、"查找..." → search_forum
        - 其他 → 日常对话
        """)
        String process(@MemoryId long memoryId, @UserMessage String userMessage);
    }

    public Map<String, Object> execute(BlogAgentState state) {
        String userMessage = state.getUserMessage();
        String sessionId = state.getSessionId();
        long memoryId = (long) sessionId.hashCode();

        log.info("[IntentAnalysis] sessionId={}, message={}", sessionId, truncate(userMessage, 80));

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("intent")
            .message("正在分析您的需求...")
            .build());

        try {
            String rawOutput = intentAgent.process(memoryId, userMessage);
            String output = stripJsonFences(rawOutput).trim();

            if (output.startsWith("{") || output.startsWith("[")) {
                // 任务路由 JSON（兜底：LLM 可能返回数组 [{...}]）
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Map<String, Object> analysis;
                if (output.startsWith("[")) {
                    Map[] arr = mapper.readValue(output, Map[].class);
                    analysis = (Map) (arr.length > 0 ? arr[0] : Map.of("intent", "chat"));
                } else {
                    analysis = mapper.readValue(output, Map.class);
                }

                String intent = (String) analysis.getOrDefault("intent", "chat");
                String topic = (String) analysis.getOrDefault("topic", "");
                String keywords = extractString(analysis.get("keywords"));
                String requirements = extractString(analysis.get("requirements"));

                log.info("[IntentAnalysis] 路由: intent={}, topic={}, requirements={}", intent, topic, requirements);

                eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                    .type("intent")
                    .message(intentLabel(intent))
                    .data(output)
                    .build());

                return Map.of(
                    BlogAgentState.KEY_INTENT, intent,
                    BlogAgentState.KEY_TOPIC, topic != null ? topic : "",
                    BlogAgentState.KEY_KEYWORDS, keywords != null ? keywords : "",
                    BlogAgentState.KEY_REQUIREMENTS, requirements != null ? requirements : ""
                );

            } else {
                // 日常对话 — 直接回复文本
                log.info("[IntentAnalysis] 对话回复: {}", truncate(output, 80));

                eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                    .type("chat")
                    .message(output)
                    .build());

                return Map.of(
                    BlogAgentState.KEY_INTENT, "chat",
                    BlogAgentState.KEY_CHAT_RESPONSE, output
                );
            }

        } catch (Exception e) {
            log.error("[IntentAnalysis] 分析失败", e);
            return Map.of(BlogAgentState.KEY_INTENT, "chat");
        }
    }

    private String intentLabel(String intent) {
        return switch (intent) {
            case "write_blog" -> "开始为您撰写博客...";
            case "search_forum" -> "正在为您搜索论坛...";
            default -> "意图识别完成";
        };
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    // LLM 可能把 keywords 输出成字符串或数组，统一转成字符串
    private String extractString(Object value) {
        if (value == null) return "";
        if (value instanceof String s) return s;
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("");
        }
        return value.toString();
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
