package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户详情实体类
 * 存储用户的扩展信息：头像、年龄、个人简介等
 * 与 sys_user 表是一对一关系，通过 user_id 关联
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_detail")
public class UserDetail {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID - 关联 sys_user 表
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
}
