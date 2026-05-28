package com.project.demo.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 好友/申请信息的响应 DTO
 *
 * 注意：这个 DTO 同时用于「好友列表」和「申请列表」两个接口
 * 根据场景不同，friendId 和 friendUsername 的含义略有不同：
 * - 好友列表：friendId = 好友的用户ID
 * - 申请列表：friendId = 申请人的用户ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {

    /**
     * 好友关系记录 ID
     * 用于后续的「同意/拒绝/删除」操作
     */
    private Long id;

    /**
     * 发起方用户ID
     * 好友列表中 = 当前用户的ID（查询条件）
     * 申请列表中 = 申请人的用户ID
     */
    private Long userId;

    /**
     * 对方用户ID
     * 好友列表中 = 好友的ID
     * 申请列表中 = 申请人的ID
     */
    private Long friendId;

    /**
     * 对方用户名
     * 关联查询 sys_user_detail 表得到
     */
    private String friendUsername;

    /**
     * 对方头像URL
     */
    private String friendAvatar;

    /**
     * 对方年龄
     */
    private Integer friendAge;

    /**
     * 对方个人简介
     */
    private String friendBio;

    /**
     * 状态码
     * 0 = 待确认，1 = 已同意，2 = 已拒绝，3 = 已删除
     */
    private Integer status;

    /**
     * 申请留言
     */
    private String message;

    /**
     * 申请时间 / 成为好友时间
     */
    private LocalDateTime createTime;
}
