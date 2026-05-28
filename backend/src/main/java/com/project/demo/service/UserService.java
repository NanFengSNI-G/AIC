package com.project.demo.service;

import com.project.demo.dto.*;
import com.project.demo.entity.User;

import java.util.List;

public interface UserService {

    void sendVerifyCode(SendCodeRequest request);

    void register(RegisterRequest request);

    TokenPairResponse login(LoginRequest request);

    TokenPairResponse loginByEmail(LoginByEmailRequest request);

    void logout(String token);

    void forgotPassword(ForgotPasswordRequest request);

    User getCurrentUser(String username);

    /**
     * 搜索用户（用于添加好友）
     *
     * @param keyword      搜索关键字
     * @param pageRequest  分页参数
     * @param currentUserId 当前登录用户ID（排除自己）
     * @return 搜索结果
     */
    PageResponse<UserSearchResponse> searchUsers(String keyword, PageRequest pageRequest, Long currentUserId);

    /**
     * 更新个人资料
     *
     * @param userId  当前用户ID
     * @param request 更新内容
     */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 获取自己的资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    UserSearchResponse getMyProfile(Long userId);
}