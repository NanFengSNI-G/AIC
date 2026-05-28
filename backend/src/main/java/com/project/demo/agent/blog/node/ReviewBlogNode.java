package com.project.demo.agent.blog.node;

import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.dto.BlogAgentSSEEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 中断节点：向用户展示生成的博客，等待发布/存草稿决策。
 * <p>
 * 纯动作节点，不包含内部路由逻辑。中断由 {@code interruptAfter("reviewBlog")} + {@code interruptBeforeEdge(true)} 接管，
 * 恢复时条件边根据 {@code userDecision} 路由到 publishBlog 或 saveDraft。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewBlogNode {

    private final BlogAgentEventManager eventManager;

    public Map<String, Object> execute(BlogAgentState state) {
        String fullBlog = state.getFullBlog();
        String blogTitle = state.getBlogTitle();
        String sessionId = state.getSessionId();

        log.info("[ReviewBlog] 博客生成完成，推送审核预览: title={}", blogTitle);

        String reviewData;
        try {
            reviewData = "{\"sessionId\":\"" + sessionId + "\",\"blogContent\":" +
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(fullBlog) + "}";
        } catch (Exception e) {
            reviewData = "{\"sessionId\":\"" + sessionId + "\",\"blogContent\":\"\"}";
        }

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("review")
            .message("博客已生成，请选择发布或保存为草稿")
            .data(reviewData)
            .build());

        // 不改变状态，中断和路由完全由框架 interruptAfter + interruptBeforeEdge 管理
        return Map.of();
    }
}
