package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User {

    /**
     * 用户ID - 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名 - 唯一约束
     */
    private String username;

    /**
     * 密码 - BCrypt加密存储
     * 注意: 在JSON序列化时忽略此字段, 防止密码泄露
     */
    private String password;

    /**
     * 邮箱 - 唯一约束
     */
    private String email;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 用户拥有的角色列表 (非数据库字段)
     */
    @TableField(exist = false)
    private List<String> roles;
}
