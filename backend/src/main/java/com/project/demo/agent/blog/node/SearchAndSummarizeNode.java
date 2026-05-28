package com.project.demo.agent.blog.node;

import com.project.demo.agent.blog.BlogAgentEventManager;
import com.project.demo.agent.blog.BlogAgentState;
import com.project.demo.agent.blog.agent.SearchAgent;
import com.project.demo.dto.BlogAgentSSEEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAndSummarizeNode {

    private final SearchAgent searchAgent;
    private final BlogAgentEventManager eventManager;

    public Map<String, Object> execute(BlogAgentState state) {
        String userMessage = state.getUserMessage();
        String sessionId = state.getSessionId();
        long memoryId = (long) sessionId.hashCode();

        log.info("[SearchNode] sessionId={}, query={}", sessionId, userMessage);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("search")
            .message("正在检索论坛内容...")
            .build());

        String summary = searchAgent.search(memoryId, userMessage);

        eventManager.send(sessionId, BlogAgentSSEEvent.builder()
            .type("search")
            .message(summary)
            .build());

        return Map.of(
            BlogAgentState.KEY_SEARCH_RESULTS, summary,
            BlogAgentState.KEY_CHAT_RESPONSE, summary
        );
    }
}
