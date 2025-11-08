package com.example.english_exam.services;

import com.example.english_exam.models.EmailVerification;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.EmailVerificationRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.util.EmailUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailUtil emailUtil;

    /**
     * Gửi link xác thực email khi người dùng đăng ký mới
     */
    @Transactional
    public void createVerification(User user) {
        try {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusHours(24);

            EmailVerification verification = new EmailVerification();
            verification.setUserId(user.getUserId()); // ✅ dùng userId thay vì user entity
            verification.setToken(token);
            verification.setExpiresAt(expiry);
            verification.setStatus("PENDING");
            emailVerificationRepository.save(verification);

            emailUtil.sendVerificationEmail(user.getEmail(), token);

        } catch (Exception e) {
            // ⚠️ Nếu gửi email lỗi, xóa luôn user + token vừa tạo
            emailVerificationRepository.deleteByUserId(user.getUserId());
            userRepository.deleteById(user.getUserId());
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng kiểm tra địa chỉ email.");
        }
    }

    /**
     * Xác thực tài khoản qua token trong email
     */
    public ResponseEntity<Map<String, Object>> verifyToken(String token) {
        // 🔍 Tìm token trong bảng email_verifications
        EmailVerification ev = emailVerificationRepository.findByToken(token).orElse(null);

        if (ev == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token không hợp lệ hoặc đã được sử dụng"));
        }

        // 🔎 Tìm user tương ứng
        User user = userRepository.findById(ev.getUserId()).orElse(null);
        if (user == null) {
            // Xóa token mồ côi nếu không còn user
            emailVerificationRepository.delete(ev);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Không tìm thấy tài khoản liên kết với token này."));
        }

        // ⏰ Kiểm tra token hết hạn
        if (ev.getExpiresAt().isBefore(LocalDateTime.now())) {
            ev.setStatus("EXPIRED");
            emailVerificationRepository.save(ev);

            // Xóa user chưa xác thực nếu token hết hạn
            if (!user.getVerified()) {
                userRepository.delete(user);
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Liên kết xác thực đã hết hạn. Tài khoản đã bị hủy, vui lòng đăng ký lại."));
        }

        // ✅ Kiểm tra trạng thái đã xác thực trước đó
        if ("VERIFIED".equals(ev.getStatus()) || user.getVerified()) {
            // Dọn sạch bản ghi cũ nếu vẫn còn
            emailVerificationRepository.delete(ev);
            return ResponseEntity.ok(Map.of("message", "Tài khoản đã được xác thực trước đó."));
        }

        // 🎯 Xác thực thành công
        user.setVerified(true);
        userRepository.save(user);

        // 🧹 Xóa token ngay sau khi xác thực
        emailVerificationRepository.delete(ev);

        return ResponseEntity.ok(Map.of("message", "✅ Xác thực tài khoản thành công!"));
    }


    /**
     * Dọn dẹp user chưa xác thực sau khi token hết hạn
     */
    public void cleanExpiredVerifications() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailVerification> expired = emailVerificationRepository
                .findAllByExpiresAtBeforeAndStatus(now, "PENDING");

        for (EmailVerification ev : expired) {
            User user = userRepository.findById(ev.getUserId()).orElse(null);
            if (user != null && !user.getVerified()) {
                emailVerificationRepository.delete(ev);
                userRepository.delete(user);
                System.out.println("🧹 Đã xóa user chưa xác thực: " + user.getEmail());
            }
        }
    }
}
