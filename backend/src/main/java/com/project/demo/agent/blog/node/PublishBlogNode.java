package com.project.demo.agent.blog.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.dto.BlogAgentSSEEvent;
import com.project.demo.dto.CreatePostRequest;
import com.project.demo.dto.PostResponse;
import com.project.demo.service.ForumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishBlogNode {

    private final ForumService forumService;
    private final BlogAgentEventManager eventManager;
    private final ObjectMapper objectMapper;

    public Map<String, Object> execute(BlogAgentState state) {
        Long userId = state.getUserId();
        Long sectionId = state.getSectionId();
        String blogTitle = state.getBlogTitle();
        String fullBlog = state.getFullBlog();
        String sessionId = state.getSessionId();

        log.info("[PublishBlog] userId={}, title={}", userId, blogTitle);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("publish")
            .message("正在发布博客...")
            .build());

        try {
            CreatePostRequest request = new CreatePostRequest();
            request.setSectionId(sectionId);
            request.setTitle(blogTitle);
            request.setContent(fullBlog);

            PostResponse response = forumService.createPost(userId, request);

            String postData = objectMapper.writeValueAsString(response);
            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("publish_result")
                .message("博客发布成功！")
                .data(postData)
                .build());

            log.info("[PublishBlog] 发布成功: postId={}", response.getId());
            return Map.of();

        } catch (Exception e) {
            log.error("[PublishBlog] 发布失败", e);
            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("error")
                .message("博客发布失败: " + e.getMessage())
                .build());
            throw new RuntimeException("博客发布失败", e);
        }
    }
}
