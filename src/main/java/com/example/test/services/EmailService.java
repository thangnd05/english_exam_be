package com.example.test.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    //JavaMailSender:
    //Đây là công cụ gửi email được cung cấp bởi Spring Boot, thường được cấu hình thông qua file application.properties hoặc application.yml.
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);          // Địa chỉ email người nhận
        message.setSubject(subject); // Tiêu đề email
        message.setText(content);   // Nội dung email
        mailSender.send(message);   // Gửi email thông qua JavaMailSender
    }
}
