package com.project.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码请求
 * 用于:
 * 1. 注册时发送验证码
 * 2. 邮箱登录时发送验证码
 * 3. 忘记密码时发送验证码
 */
@Data
public class SendCodeRequest {

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码类型
     * - register: 注册
     * - login: 邮箱登录
     * - reset-password: 重置密码
     */
    @NotBlank(message = "类型不能为空")
    private String type;
}
