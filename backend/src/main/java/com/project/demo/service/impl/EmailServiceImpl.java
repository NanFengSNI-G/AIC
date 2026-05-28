package com.project.demo.service.impl;

import com.project.demo.config.EmailConfig;
import com.project.demo.exception.BusinessException;
import com.project.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务实现
 * 使用 Spring Mail 发送邮件
 */
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailConfig emailConfig;

    /**
     * 发送验证码邮件
     */
    @Override
    public void sendVerifyCodeEmail(String to, String code, String subject, String content) {
        try {
            // ========== 1. 创建邮件消息 ==========
            MimeMessage message = mailSender.createMimeMessage();

            // ========== 2. 配置邮件内容 ==========
            //第二个参数 true 表示启用 HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);

            // ========== 3. 设置邮件内容 (HTML格式) ==========
            // 验证码5分钟内有效
            helper.setText(buildHtmlContent(code), true);

            // ========== 4. 发送邮件 ==========
            mailSender.send(message);

        } catch (Exception e) {
            throw BusinessException.serverError("发送邮件失败: " + e.getMessage());
        }
    }

    /**
     * 构建HTML邮件内容
     *
     * @param code 验证码
     * @return HTML内容
     */
    private String buildHtmlContent(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                    .container { max-width: 400px; margin: 50px auto; padding: 20px;
                                 background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .code { font-size: 32px; font-weight: bold; color: #4CAF50;
                            letter-spacing: 8px; text-align: center; margin: 30px 0; }
                    .warning { font-size: 12px; color: #888; text-align: center; margin-top: 20px; }
                    h2 { color: #333; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>您的验证码</h2>
                    <div class="code">%s</div>
                    <p style="text-align:center;color:#666;">
                        请在5分钟内完成验证，此验证码仅限一次性使用。
                    </p>
                    <p class="warning">
                        如果您没有进行相关操作，请忽略此邮件。
                    </p>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
