package com.example.aiserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public void sendPasswordResetCode(String email, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(mailFrom)) {
            log.warn("Password reset code for {} is {}. SMTP is not configured, email was not sent.", email, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("二次元记录站密码重置验证码");
            message.setText("您的密码重置验证码是：" + code + "。验证码 5 分钟内有效，请勿泄露给他人。");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send password reset code email to {}. Code is {}.", email, code, ex);
        }
    }
}
