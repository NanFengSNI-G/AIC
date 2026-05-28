package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索用户响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 好友关系状态
     * none - 无关系
     * friends - 已是好友
     * request_sent - 已发送申请（待对方确认）
     * request_received - 收到对方申请（待自己确认）
     */
    private String friendStatus;
}