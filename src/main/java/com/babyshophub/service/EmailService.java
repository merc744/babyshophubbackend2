package com.babyshophub.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean consoleMode;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.console-mode:false}") boolean consoleMode) {
        this.mailSender = mailSender;
        this.consoleMode = consoleMode;
    }

    public boolean sendVerificationEmail(String toEmail, String verificationCode) {
        if (consoleMode) {
            LOGGER.info("Local verification code for {}: {}", toEmail, verificationCode);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("BabyShopHub - Email Verification Code");
        message.setText("Your verification code is: " + verificationCode + 
                        "\n\nPlease use this code to activate your account. It expires in 15 minutes.");
        mailSender.send(message);
        return true;
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetCode) {
        if (consoleMode) {
            LOGGER.info("Local password reset code for {}: {}", toEmail, resetCode);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("BabyShopHub - Password Reset Code");
        message.setText("Your password reset code is: " + resetCode
                + "\n\nEnter this code to reset your password. It expires in 10 minutes."
                + " If you did not request a reset, you can ignore this email.");
        mailSender.send(message);
        return true;
    }
}
