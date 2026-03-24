package com.example.english_exam.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationEmail(String email, String token) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // 🟢 Thêm người gửi (rất quan trọng để Gmail không đánh dấu spam)
        try {
            helper.setFrom("Thang10072005@gmail.com", "WinDe Exam");
        } catch (UnsupportedEncodingException e) {
            helper.setFrom("Thang10072005@gmail.com");
        }
        helper.setTo(email);
        helper.setSubject("🔐 Xác thực tài khoản WinDe Exam");

        // 🧩 HTML nội dung email (nút đẹp, giao diện gọn)
        String content = """
            <div style="font-family:Arial, sans-serif; background:#f9f9f9; padding:20px; border-radius:10px;">
                <h2 style="color:#2c3e50;">Xin chào!</h2>
                <p style="font-size:16px; color:#333;">
                    Cảm ơn bạn đã đăng ký tài khoản tại <b>WinDe Exam</b>.<br>
                    Vui lòng ấn vào nút bên dưới để xác thực tài khoản của bạn:
                </p>
                <div style="margin:25px 0;">
                    <a href="http://localhost:3000/verify?token=%s"
                       style="background-color:#00c6ff; color:white; text-decoration:none;
                              padding:12px 20px; border-radius:8px; font-weight:bold;">
                        Xác thực ngay
                    </a>
                </div>
                <p style="color:#666; font-size:13px;">
                    Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.<br><br>
                    Trân trọng,<br>
                    <b>Đội ngũ WinDe Exam</b>
                </p>
            </div>
        """.formatted(token);

        helper.setText(content, true);

        try {
            javaMailSender.send(mimeMessage);
            System.out.println("✅ Sent verification email to " + email);
        } catch (MailException e) {
            System.err.println("❌ Lỗi khi gửi mail: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng kiểm tra địa chỉ email.");
        }
    }

    public void sendResetPasswordEmail(String email, String token) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            try {
                helper.setFrom("Thang10072005@gmail.com", "WinDe Exam");
            } catch (UnsupportedEncodingException e) {
                helper.setFrom("Thang10072005@gmail.com");
            }
            helper.setTo(email);
            helper.setSubject("Đặt lại mật khẩu WinDe Exam");

            String content = """
                <div style="font-family:Arial, sans-serif; background:#f9f9f9; padding:20px; border-radius:10px;">
                    <h2 style="color:#2c3e50;">Yêu cầu đặt lại mật khẩu</h2>
                    <p style="font-size:16px; color:#333;">
                        Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.<br>
                        Vui lòng nhấn nút bên dưới để tiếp tục:
                    </p>
                    <div style="margin:25px 0;">
                        <a href="http://localhost:3000/reset-password?token=%s"
                           style="background-color:#ff7a18; color:white; text-decoration:none;
                                  padding:12px 20px; border-radius:8px; font-weight:bold;">
                            Đặt lại mật khẩu
                        </a>
                    </div>
                    <p style="color:#666; font-size:13px;">
                        Liên kết có hiệu lực trong 30 phút.<br>
                        Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.<br><br>
                        Trân trọng,<br>
                        <b>Đội ngũ WinDe Exam</b>
                    </p>
                </div>
            """.formatted(token);

            helper.setText(content, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu.");
        }
    }
}
