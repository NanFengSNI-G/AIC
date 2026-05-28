package com.project.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 更新个人资料请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /**
     * 用户名（可选）
     */
    private String username;

    /**
     * 头像URL（可选）
     */
    private String avatar;

    /**
     * 年龄（可选）
     */
    private Integer age;

    /**
     * 个人简介（可选）
     */
    @Size(max = 500, message = "个人简介不能超过500字符")
    private String bio;
}
