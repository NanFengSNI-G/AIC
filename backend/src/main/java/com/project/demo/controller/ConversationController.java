package com.project.demo.controller;

import com.project.demo.dto.ConversationMessage;
import com.project.demo.dto.ConversationSummary;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/blog/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 新建对话，返回 sessionId。
     */
    @PostMapping
    public ResponseEntity<ConversationSummary> create(@RequestParam(required = false) String title) {
        Long userId = getCurrentUserId();
        ConversationSummary summary = conversationService.create(userId, title);
        return ResponseEntity.ok(summary);
    }

    /**
     * 查询当前用户的历史对话列表。
     */
    @GetMapping
    public ResponseEntity<List<ConversationSummary>> list() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(conversationService.list(userId));
    }

    /**
     * 读取指定对话的聊天记录。
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<ConversationMessage>> getMessages(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(conversationService.getMessages(userId, sessionId));
    }

    /**
     * 删除对话。
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        conversationService.delete(userId, sessionId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return userDetails.getUserId();
    }
}
