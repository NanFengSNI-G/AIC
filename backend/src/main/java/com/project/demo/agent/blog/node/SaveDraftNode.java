package com.project.demo.agent.blog.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.dto.BlogAgentSSEEvent;
import com.project.demo.dto.CreatePostRequest;
import com.project.demo.dto.PostResponse;
import com.project.demo.entity.ForumPost;
import com.project.demo.mapper.ForumPostMapper;
import com.project.demo.service.ForumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaveDraftNode {

    private final ForumService forumService;
    private final ForumPostMapper forumPostMapper;
    private final BlogAgentEventManager eventManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> execute(BlogAgentState state) {
        Long userId = state.getUserId();
        String blogTitle = state.getBlogTitle();
        String fullBlog = state.getFullBlog();
        String sessionId = state.getSessionId();

        log.info("[SaveDraft] userId={}, title={}", userId, blogTitle);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("draft")
            .message("正在保存草稿...")
            .build());

        try {
            CreatePostRequest request = new CreatePostRequest();
            request.setSectionId(state.getSectionId());
            request.setTitle(blogTitle);
            request.setContent(fullBlog);

            PostResponse response = forumService.createPost(userId, request);

            ForumPost post = new ForumPost();
            post.setId(response.getId());
            post.setStatus(ForumPost.STATUS_DRAFT);
            forumPostMapper.update(post);

            String draftData = objectMapper.writeValueAsString(response);

            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("draft_result")
                .message("草稿已保存！")
                .data(draftData)
                .build());

            log.info("[SaveDraft] 草稿保存成功: postId={}", response.getId());
            return Map.of();

        } catch (Exception e) {
            log.error("[SaveDraft] 保存草稿失败", e);
            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("error")
                .message("草稿保存失败: " + e.getMessage())
                .build());
            throw new RuntimeException("草稿保存失败", e);
        }
    }
}
