package com.example.english_exam.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationEmail(String email, String token) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(email);
        helper.setSubject("Verify your account");
        helper.setText("""
            <div style="font-family:Arial, sans-serif;">
                <h3>Welcome!</h3>
                <p>Click the link below to verify your account:</p>
                <a href="http://localhost:3000/verify?token=%s">Verify now</a>
            </div>
        """.formatted(token), true);

        try {
            javaMailSender.send(mimeMessage);
            System.out.println("✅ Sent verification email to " + email);
        } catch (MailException e) {
            System.err.println("❌ Lỗi khi gửi mail: " + e.getMessage());
            // ép throw để trigger catch trong EmailVerificationService
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng kiểm tra địa chỉ email.");
        }
    }
}
