package com.project.demo.controller;

import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentGraph;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.dto.BlogAgentRequest;
import com.project.demo.dto.BlogAgentResumeRequest;
import com.project.demo.dto.BlogAgentSSEEvent;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Blog Agent SSE 控制器 — 企业级 Human-in-the-Loop。
 *
 * <pre>
 * POST /api/blog/chat   → SSE（sessionId 由前端提供，需先通过 /api/blog/conversations 创建）
 * POST /api/blog/resume  → SSE（用户决策后恢复，发布或存草稿）
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/blog")
public class BlogAgentController {

    private final BlogAgentGraph blogAgentGraph;
    private final BlogAgentEventManager eventManager;
    private final ConversationService conversationService;

    public BlogAgentController(BlogAgentGraph blogAgentGraph,
                               BlogAgentEventManager eventManager,
                               ConversationService conversationService) {
        this.blogAgentGraph = blogAgentGraph;
        this.eventManager = eventManager;
        this.conversationService = conversationService;
    }

    /**
     * 发起对话，返回 SSE 流。sessionId 由调用方提供。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody BlogAgentRequest request) {
        Long userId = getCurrentUserId();
        String sessionId = request.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空，请先调用 POST /api/blog/conversations 创建会话");
        }

        // 校验会话归属
        conversationService.validateOwnership(userId, sessionId);
        // 更新会话元数据（标题、时间）
        conversationService.touch(userId, sessionId, request.getMessage());

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        eventManager.register(sessionId, emitter);

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> init = Map.of(
                    BlogAgentState.KEY_USER_ID, userId,
                    BlogAgentState.KEY_SESSION_ID, sessionId,
                    BlogAgentState.KEY_USER_MESSAGE, request.getMessage() != null ? request.getMessage() : "",
                    BlogAgentState.KEY_SECTION_ID, request.getSectionId() != null ? request.getSectionId() : 1L
                );
                BlogAgentState state = new BlogAgentState(init);

                Optional<BlogAgentState> result = blogAgentGraph.invoke(state, sessionId);

                if (result.isEmpty()) {
                    eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                        .type("error")
                        .message("工作流未返回结果")
                        .build());
                } else {
                    BlogAgentState finalState = result.get();
                    log.info("[BlogAgent] invoke 完成: sessionId={}, intent={}, decision={}",
                        sessionId, finalState.getIntent(), finalState.getUserDecision());

                    if ("write_blog".equals(finalState.getIntent()) && finalState.getUserDecision() == null) {
                        log.info("[BlogAgent] 图已中断于 reviewBlog，等待用户决策: sessionId={}", sessionId);
                    }
                }

            } catch (Exception e) {
                log.error("[BlogAgent] chat 异常: sessionId={}", sessionId, e);
                eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                    .type("error")
                    .message("处理失败: " + e.getMessage())
                    .build());
                eventManager.error(sessionId, e);
            } finally {
                try {
                    eventManager.complete(sessionId);
                } catch (Exception ignored) {
                }
            }
        });

        emitter.onCompletion(() -> eventManager.unregister(sessionId));
        emitter.onTimeout(() -> {
            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("error")
                .message("处理超时，请重试")
                .build());
            eventManager.error(sessionId, new RuntimeException("超时"));
        });

        return emitter;
    }

    /**
     * 恢复中断的博客生成流程。用户决策 {@code PUBLISH} 或 {@code DRAFT}。
     */
    @PostMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter resume(@RequestBody BlogAgentResumeRequest request) {
        Long userId = getCurrentUserId();
        String sessionId = request.getSessionId();
        String decision = request.getDecision();

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (!"PUBLISH".equals(decision) && !"DRAFT".equals(decision)) {
            throw new IllegalArgumentException("decision 必须为 PUBLISH 或 DRAFT");
        }

        // 校验会话归属（使用 ConversationService 统一校验）
        conversationService.validateOwnership(userId, sessionId);

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        eventManager.register(sessionId, emitter);

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> init = Map.of(
                    BlogAgentState.KEY_USER_ID, userId,
                    BlogAgentState.KEY_SESSION_ID, sessionId,
                    BlogAgentState.KEY_USER_DECISION, decision,
                    BlogAgentState.KEY_SECTION_ID, request.getSectionId() != null ? request.getSectionId() : 1L
                );
                BlogAgentState state = new BlogAgentState(init);

                log.info("[BlogAgent] resume: sessionId={}, decision={}", sessionId, decision);
                Optional<BlogAgentState> result = blogAgentGraph.resume(state, sessionId);

                if (result.isPresent()) {
                    log.info("[BlogAgent] resume 完成: sessionId={}, decision={}",
                        sessionId, result.get().getUserDecision());
                }

            } catch (Exception e) {
                log.error("[BlogAgent] resume 异常: sessionId={}", sessionId, e);
                eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                    .type("error")
                    .message("恢复失败: " + e.getMessage())
                    .build());
                eventManager.error(sessionId, e);
            } finally {
                try {
                    eventManager.complete(sessionId);
                } catch (Exception ignored) {
                }
            }
        });

        emitter.onCompletion(() -> eventManager.unregister(sessionId));
        emitter.onTimeout(() -> {
            eventManager.send(sessionId, BlogAgentSSEEvent.builder()
                .type("error")
                .message("处理超时")
                .build());
            eventManager.error(sessionId, new RuntimeException("超时"));
        });

        return emitter;
    }

    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return userDetails.getUserId();
    }
}
