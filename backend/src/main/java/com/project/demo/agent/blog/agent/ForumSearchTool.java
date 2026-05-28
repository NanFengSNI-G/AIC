package com.project.demo.agent.blog.agent;

import com.project.demo.dto.PageResponse;
import com.project.demo.dto.PostResponse;
import com.project.demo.service.ForumService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForumSearchTool {

    private final ForumService forumService;

    /**
     * 在论坛中按关键词检索帖子。Agent 可多次调用以获取足够信息。
     *
     * @param keyword 搜索关键词
     * @return 格式化的检索结果，包含帖子标题、作者和内容摘要；未找到时返回提示信息
     */
    @Tool("在论坛中检索帖子。传入关键词，返回匹配的帖子列表（含标题、作者、内容摘要）。未找到时返回提示。")
    public String searchForum(String keyword) {
        log.info("[ForumSearchTool] 检索: keyword={}", keyword);

        try {
            PageResponse<PostResponse> result = forumService.searchPosts(
                null, keyword, null, "relevance", 1, 5);

            if (result == null || result.getList().isEmpty()) {
                return "未找到与「" + keyword + "」相关的帖子。建议尝试其他关键词。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(result.getTotal()).append(" 个相关帖子（显示前 ")
              .append(result.getList().size()).append(" 个）：\n\n");

            for (int i = 0; i < result.getList().size(); i++) {
                PostResponse post = result.getList().get(i);
                sb.append("---\n");
                sb.append("标题: ").append(post.getTitle()).append("\n");
                sb.append("作者: ").append(post.getAuthorUsername()).append("\n");
                sb.append("内容摘要: ").append(truncate(stripMarkdown(post.getContent()), 200)).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("[ForumSearchTool] 检索失败", e);
            return "检索时出错: " + e.getMessage() + "。请稍后重试。";
        }
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private String stripMarkdown(String md) {
        return md.replaceAll("[#*`\\[\\]()_~>]", "").replaceAll("\\s+", " ");
    }
}
