package com.project.demo.controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.dto.ConversationResponse;
import com.project.demo.dto.ChatMessageResponse;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * GET /api/chat/conversations
     * 获取最近会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResponse.success(chatService.getConversations(user.getUserId()));
    }

    /**
     * GET /api/chat/history
     * 获取与好友的聊天历史 (分页)
     * 参数: friendId, page(默认1), size(默认20)
     */
    @GetMapping("/history")
    public ApiResponse<List<ChatMessageResponse>> getHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long friendId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(chatService.getHistory(user.getUserId(), friendId, page, size));
    }

    /**
     * PUT /api/chat/read?friendId={id}
     * 标记与好友的所有消息为已读
     */
    @PutMapping("/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long friendId) {
        chatService.markAsRead(user.getUserId(), friendId);
        return ApiResponse.success(null);
    }

    /**
     * GET /api/chat/unread/total
     * 获取未读消息总数
     */
    @GetMapping("/unread/total")
    public ApiResponse<Integer> getUnreadTotal(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiResponse.success(chatService.getUnreadTotal(user.getUserId()));
    }

}
