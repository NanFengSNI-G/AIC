package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送好友申请的请求参数
 * 为什么只有两个字段？
 * → 前端需要告诉后端：我要给「谁」发申请，以及「说什么」
 * → 发起者的身份是从 JWT Token 中解析出来的，不需要前端传
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequest {

    /**
     * 目标用户ID
     * 即我要添加的好友的用户ID
     * 前端怎么知道对方的 userId？
     * → 用户在「搜索用户」功能中，根据用户名/邮箱查询，拿到 userId
     */
    private Long targetUserId;

    /**
     * 申请留言
     * 可选字段，用户可以填写自我介绍
     * 为什么设计为可选？
     * → 不是所有场景都需要留言，比如扫码加好友
     * → 如果不填，后端存空字符串 ""
     */
    private String message;
}
