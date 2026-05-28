package com.project.demo.controller;

import com.project.demo.dto.*;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    private static final String ONLINE_USERS_SET = "online:users";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/online-status")
    public ResponseEntity<ApiResponse<List<Long>>> getOnlineUserIds(
            @AuthenticationPrincipal UserDetailsImpl user) {

        List<Long> result = new ArrayList<>();
        try (Cursor<Object> cursor = redisTemplate.opsForSet()
                .scan(ONLINE_USERS_SET, ScanOptions.NONE)) {
            cursor.forEachRemaining(id -> result.add(((Number) id).longValue()));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/send-friend")
    public ResponseEntity<ApiResponse<Void>> sendFriendRequest(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody SendFriendRequest request) {
        friendService.sendFriendRequest(user.getUserId(), request.getTargetUserId(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.<Void>success("好友申请已发送", null));
    }

    @PostMapping("/handle-friend")
    public ResponseEntity<ApiResponse<Void>> handleFriendRequest(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody HandleFriendRequest request) {
        friendService.handleFriendRequest(user.getUserId(), request.getId(), request.getAccept());
        String message = request.getAccept() ? "已同意" : "已拒绝";
        return ResponseEntity.ok(ApiResponse.<Void>success(message, null));
    }

    @PostMapping("/list-friend")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponse>>> getFriendList(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody PageRequest pageRequest) {
        PageResponse<FriendResponse> result = friendService.getFriendList(user.getUserId(), pageRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/list-friend-requests")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponse>>> getFriendRequests(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody PageRequest pageRequest) {
        PageResponse<FriendResponse> result = friendService.getFriendRequests(user.getUserId(), pageRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/delete-relation")
    public ResponseEntity<ApiResponse<Void>> deleteFriendByRelationId(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long relationId) {
        friendService.deleteFriendByRelationId(user.getUserId(), relationId);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }
}
