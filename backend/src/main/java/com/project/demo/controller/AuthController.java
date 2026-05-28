package com.project.demo.controller;

import com.project.demo.dto.*;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.service.UserService;
import com.project.demo.utils.JwtUtils;
import com.project.demo.utils.RedisUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理: 发送验证码、注册、登录、退出、忘记密码、搜索用户、资料更新
 * 路径: /api/auth/**
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisUtils redisUtils;

    private static final String TOKEN_PREFIX = "Bearer ";

    // ========== 发送验证码 ==========

    /**
     * 发送验证码
     * POST /api/auth/send-code
     * Body: {"email": "xxx@xxx.com", "type": "register|login|reset-password"}
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(
            @Valid @RequestBody SendCodeRequest request) {
        userService.sendVerifyCode(request);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送"));
    }

    // ========== 注册 ==========

    /**
     * 注册
     * POST /api/auth/register
     * Body: {"username": "xxx", "email": "xxx", "password": "xxx", "code": "123456"}
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    // ========== 登录 ==========

    /**
     * 用户名密码登录
     * POST /api/auth/login
     * Body: {"username": "xxx", "password": "xxx"}
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenPairResponse tokenPairResponse = userService.login(request);

        Map<String, String> data = new HashMap<>();
        data.put("token", tokenPairResponse.getToken());
        data.put("refreshToken", tokenPairResponse.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("登录成功", data));
    }

    /**
     * 邮箱验证码登录
     * POST /api/auth/login-by-email
     * Body: {"email": "xxx@xxx.com", "code": "123456"}
     */
    @PostMapping("/login-by-email")
    public ResponseEntity<ApiResponse<Map<String, String>>> loginByEmail(
            @Valid @RequestBody LoginByEmailRequest request) {
        TokenPairResponse tokenPairResponse = userService.loginByEmail(request);

        Map<String, String> data = new HashMap<>();
        data.put("token", tokenPairResponse.getToken());
        data.put("refreshToken", tokenPairResponse.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("登录成功", data));
    }

    // ========== 退出登录 ==========

    /**
     * 退出登录
     * POST /api/auth/logout
     * Header: Authorization: Bearer <token>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());
        userService.logout(token);

        return ResponseEntity.ok(ApiResponse.success("退出成功"));
    }

    // ========== 忘记密码 ==========

    /**
     * 忘记密码 - 重置密码
     * POST /api/auth/forgot-password
     * Body: {"email": "xxx@xxx.com", "code": "123456", "newPassword": "xxx"}
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>success("密码重置成功", null));
    }

    // ========== 刷新 Token ==========

    /**
     * 刷新 Token
     * POST /api/auth/refresh-token
     * Header: Authorization: Bearer <refresh_token>
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            return ResponseEntity.ok(ApiResponse.error(401, "未提供 Refresh Token"));
        }

        String refreshToken = authHeader.substring(TOKEN_PREFIX.length());

        if (!jwtUtils.validateToken(refreshToken)) {
            return ResponseEntity.ok(ApiResponse.error(401, "Refresh Token 无效或已过期"));
        }

        if (!"refresh".equals(jwtUtils.getTokenType(refreshToken))) {
            return ResponseEntity.ok(ApiResponse.error(401, "Token 类型错误"));
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);

        String storedToken = redisUtils.getRefreshToken(userId);
        if (!refreshToken.equals(storedToken)) {
            return ResponseEntity.ok(ApiResponse.error(401, "Refresh Token 已失效"));
        }

        String newAccessToken = jwtUtils.generateToken(username, userId);

        // 重置 Redis TTL 为 24h 闲置窗口（30天绝对过期由 JWT exp 保证）
        final long idleSeconds = JwtUtils.REFRESH_IDLE_SECONDS;
        redisUtils.storeRefreshToken(userId, refreshToken, idleSeconds);
        UserDetailsImpl userDetails = redisUtils.getUserDetails(userId);
        if (userDetails != null) {
            redisUtils.storeUserDetails(userId, userDetails, idleSeconds);
        }

        Map<String, String> data = new HashMap<>();
        data.put("token", newAccessToken);

        return ResponseEntity.ok(ApiResponse.success("刷新成功", data));
    }

    // ========== 搜索用户 ==========

    /**
     * 搜索用户
     * GET /api/auth/search-users?keyword=xxx&page=1&size=20
     * 用于添加好友时查找用户
     */
    @GetMapping("/search-users")
    public ResponseEntity<ApiResponse<PageResponse<UserSearchResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl user) {
        PageRequest pageRequest = new PageRequest(page, size, null);
        PageResponse<UserSearchResponse> result = userService.searchUsers(keyword, pageRequest, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 个人资料 ==========

    /**
     * 获取当前用户资料
     * GET /api/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserSearchResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(user.getUserId())));
    }

    /**
     * 更新个人资料
     * PUT /api/auth/profile
     * Body: {"username": "新名字", "avatar": "https://...", "age": 25, "bio": "简介"}
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>success("更新成功", null));
    }
}
