package com.project.demo.service.impl;

import com.project.demo.dto.*;
import com.project.demo.entity.Friend;
import com.project.demo.entity.User;
import com.project.demo.exception.BusinessException;
import com.project.demo.mapper.FriendMapper;
import com.project.demo.mapper.UserMapper;
import com.project.demo.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 好友 Service 实现
 */
@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 发送好友申请
     * 校验流程：
     * 1. 不能添加自己
     * 2. 目标用户必须存在
     * 3. 目标用户必须状态正常（未被禁用）
     * 4. 不能是已同意的好友（双向检查）
     * 5. 如果存在待确认/已拒绝/已删除的申请，可以重新发起
     */
    @Override
    @Transactional
    public void sendFriendRequest(Long userId, Long targetUserId, String message) {
        // ----- 校验1：不能添加自己 -----
        if (userId.equals(targetUserId)) {
            throw BusinessException.badRequest("不能添加自己为好友");
        }

        // ----- 校验2：目标用户必须存在 -----
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            throw BusinessException.notFound("目标用户不存在");
        }

        // ----- 校验3：目标用户必须未被禁用 -----
        if (targetUser.getStatus() != 1) {
            throw BusinessException.forbidden("该用户已禁用，无法添加好友");
        }

        // ----- 校验4+5：一次查询双向记录 -----
        List<Friend> existingList = friendMapper.selectBothWays(userId, targetUserId);
        Friend existing1 = null; // userId → targetUserId
        Friend existing2 = null; // targetUserId → userId
        for (Friend f : existingList) {
            if (f.getUserId().equals(userId)) {
                existing1 = f;
            } else {
                existing2 = f;
            }
        }

        // 任一方向是已同意状态 = 已是好友，不能再发申请
        if ((existing1 != null && existing1.getStatus() == 1) ||
            (existing2 != null && existing2.getStatus() == 1)) {
            throw BusinessException.badRequest("你们已经是好友");
        }

        // ----- 校验5：处理已存在的申请记录 -----
        if (existing1 != null) {
            // 我给 ta 发过申请（可能是拒绝或删除状态）
            existing1.setStatus(0);
            existing1.setMessage(message != null ? message : "");
            existing1.setCreateTime(java.time.LocalDateTime.now());
            friendMapper.updateById(existing1);
        } else if (existing2 != null) {
            // ta 给过我申请
            if (existing2.getStatus() == 0) {
                // 对方已发过申请且是待确认状态 → 双向都想加好友，直接同意
                friendMapper.updateStatusAndUpdateTime(existing2.getId(), 1, java.time.LocalDateTime.now());
            } else {
                // 已拒绝或已删除状态 → 重新发起申请
                existing2.setStatus(0);
                existing2.setMessage(message != null ? message : "");
                existing2.setCreateTime(java.time.LocalDateTime.now());
                friendMapper.updateById(existing2);
            }
        } else {
            // 完全没有任何记录，创建新申请
            Friend friend = new Friend();
            friend.setUserId(userId);
            friend.setFriendId(targetUserId);
            friend.setStatus(0);
            friend.setMessage(message != null ? message : "");
            friendMapper.insertFriend(friend);
        }
    }

    // ============================================================
    // 处理好友申请
    // ============================================================

    /**
     * 处理好友申请（同意/拒绝）
     * 状态流转：
     * - 同意：status = 0（待确认）→ status = 1（已同意）
     * - 拒绝：status = 0（待确认）→ status = 2（已拒绝）
     */
    @Override
    @Transactional
    public void handleFriendRequest(Long userId, Long requestId, boolean accept) {
        // ----- 校验1：申请必须存在 -----
        Friend friend = friendMapper.selectById(requestId);
        if (friend == null) {
            throw BusinessException.notFound("好友申请不存在");
        }

        // ----- 校验2：必须是发给当前用户的申请 -----
        if (!friend.getFriendId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此申请");
        }

        // ----- 校验3：必须是待确认状态 -----
        if (friend.getStatus() != 0) {
            throw BusinessException.badRequest("该申请已被处理");
        }

        // ----- 业务执行 -----
        if (accept) {
            friendMapper.updateStatusAndUpdateTime(requestId, 1, java.time.LocalDateTime.now());
        } else {
            friendMapper.updateStatus(requestId, 2);
        }
    }

    /**
     * 获取好友列表（分页，支持搜索）
     */
    @Override
    public PageResponse<FriendResponse> getFriendList(Long userId, PageRequest pageRequest) {
        // 统一调用：search 为空时传 null，SQL 中的 <if> 标签会自动忽略
        String search = pageRequest.getSearch();
        String sanitized = null;
        if (search != null && !search.trim().isEmpty()) {
            // 移除 LIKE 通配符，防止搜索结果被意外干扰
            sanitized = search.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
        }

        List<Friend> list = friendMapper.selectFriendsByUserId(
                userId,
                sanitized,
                pageRequest.getOffset(),
                pageRequest.getSize()
        );
        Long total = friendMapper.countFriendsByUserId(userId, sanitized);

        List<FriendResponse> respList = list.stream().map(this::toResponse).toList();

        return PageResponse.of(respList, total, pageRequest.getPage(), pageRequest.getSize());
    }

    // ============================================================
    // 获取申请列表
    // ============================================================

    @Override
    public PageResponse<FriendResponse> getFriendRequests(Long userId, PageRequest pageRequest) {
        // 统一调用：search 为空时传 null，SQL 中的 <if> 标签会自动忽略
        String search = pageRequest.getSearch();
        String sanitized = null;
        if (search != null && !search.trim().isEmpty()) {
            // 移除 LIKE 通配符，防止搜索结果被意外干扰
            sanitized = search.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
        }

        List<Friend> list = friendMapper.selectFriendRequestsByUserId(
                userId,
                sanitized,
                pageRequest.getOffset(),
                pageRequest.getSize()
        );
        Long total = friendMapper.countFriendRequestsByUserId(userId, sanitized);
        List<FriendResponse> respList = list.stream().map(this::toResponse).toList();

        return PageResponse.of(respList, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 删除好友（通过关系记录ID）
     * DELETE /api/friend/relation/{relationId}：
     * - relationId 是「好友关系的主键ID」
     * - 删除前校验：(user_id=当前用户 OR friend_id=当前用户) AND id=relationId
     * - 确保只有关系中的当事人才能删除
     */
    @Override
    @Transactional
    public void deleteFriendByRelationId(Long userId, Long relationId) {
        Friend friend = friendMapper.selectById(relationId);
        if (friend == null) {
            throw BusinessException.notFound("好友关系不存在");
        }

        // 校验：只有关系中的双方才能删除
        // (user_id = 当前用户 OR friend_id = 当前用户)
        boolean isParty = friend.getUserId().equals(userId) || friend.getFriendId().equals(userId);
        if (!isParty) {
            throw BusinessException.forbidden("无权删除此好友关系");
        }

        // 必须是已同意状态才能删除
        if (friend.getStatus() != 1) {
            throw BusinessException.badRequest("该关系不是好友状态，无法删除");
        }

        friendMapper.updateStatus(relationId, 3);
    }

    // ============================================================
    // 私有工具方法
    // ============================================================

    private FriendResponse toResponse(Friend f) {
        FriendResponse resp = new FriendResponse();
        resp.setId(f.getId());
        resp.setUserId(f.getUserId());
        resp.setFriendId(f.getFriendId());
        resp.setStatus(f.getStatus());
        resp.setMessage(f.getMessage());
        resp.setCreateTime(f.getCreateTime());

        if (f.getFriendDetail() != null) {
            resp.setFriendUsername(f.getFriendDetail().getUsername());
            resp.setFriendAvatar(f.getFriendDetail().getAvatar());
            resp.setFriendAge(f.getFriendDetail().getAge());
            resp.setFriendBio(f.getFriendDetail().getBio());
        }
        return resp;
    }
}
