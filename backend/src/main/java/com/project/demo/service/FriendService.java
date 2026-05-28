package com.project.demo.service;

import com.project.demo.dto.*;

/**
 * 好友 Service 接口

 * 为什么要定义接口？
 * → 接口定义「做什么」，实现类定义「怎么做」
 * → 方便切换实现（如从本地实现换成远程实现）
 * → 便于单元测试（可以注入 mock 实现）
 */
public interface FriendService {

    /**
     * 发送好友申请
     *
     * @param userId       当前登录用户ID
     * @param targetUserId 目标用户ID
     * @param message      申请留言（可为 null）
     */
    void sendFriendRequest(Long userId, Long targetUserId, String message);

    /**
     * 处理好友申请（同意/拒绝）
     *
     * @param userId    当前登录用户ID（收到申请的人）
     * @param requestId 好友关系记录ID
     * @param accept    true=同意，false=拒绝
     */
    void handleFriendRequest(Long userId, Long requestId, boolean accept);

    /**
     * 获取好友列表（分页，支持搜索）
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PageResponse<FriendResponse> getFriendList(Long userId, PageRequest pageRequest);

    /**
     * 获取收到的申请列表（分页）
     *
     * @param userId      用户ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PageResponse<FriendResponse> getFriendRequests(Long userId, PageRequest pageRequest);

    /**
     * 删除好友（通过关系记录ID）
     *
     * @param userId     当前登录用户ID
     * @param relationId 好友关系记录ID
     */
    void deleteFriendByRelationId(Long userId, Long relationId);

}