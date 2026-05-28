package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;  // 标注非数据库字段
import com.baomidou.mybatisplus.annotation.TableName;   // 指定对应的表名
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友关系实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_friend")
public class Friend {

    /**
     * 主键ID
     * 对应数据库的 BIGINT AUTO_INCREMENT
     * MyBatis-Plus 的 BaseMapper<T> 会自动提供 CRUD 方法
     */
    private Long id;

    /**
     * 用户ID（发起方）
     * 重要：这里的"发起方"指的是申请关系的发起者
     * 举例：A 给 B 发申请 → user_id=A, friend_id=B
     */
    private Long userId;

    /**
     * 好友用户ID（接收方）
     */
    private Long friendId;

    /**
     * 状态
     * 0 = 待确认（申请已发送，等待对方处理）
     * 1 = 已同意（好友关系建立）
     * 2 = 已拒绝（对方拒绝了申请）
     * 3 = 已删除（好友被删除，软删除）
     */
    private Integer status;

    /**
     * 申请留言
     * 用户在发送申请时可以填写，如"你好，我是xxx"
     * 可为空字符串 ""
     */
    private String message;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 好友的用户详情信息
     * 通过 <association> 关联查询 sys_user_detail 表
     * 包含用户名、头像、年龄、个人简介
     */
    @TableField(exist = false)
    private UserDetail friendDetail;
}