package com.project.demo.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     *
     * @param to      收件人邮箱
     * @param code    验证码
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendVerifyCodeEmail(String to, String code, String subject, String content);
}
