package com.project.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置类
 * 存储邮件相关的配置信息
 */
@Configuration
public class EmailConfig {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${email.from-name:}")
    private String fromName;

    /**
     * 获取发件人地址
     */
    public String getFrom() {
        return username;
    }

    /**
     * 获取发件人显示名称
     */
    public String getFromName() {
        return fromName != null && !fromName.isEmpty() ? fromName : username;
    }
}
