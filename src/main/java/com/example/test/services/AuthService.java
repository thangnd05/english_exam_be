package com.example.test.services;

import com.example.test.models.Users;
import com.example.test.respositories.UserRespo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRespo userRepository;
    private final EmailService emailService;

    public AuthService(UserRespo userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public String forgotPassword(String email) {
        //Tìm kiếm người dùng qua email:
        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            //lấy thông tin người dùng ra (userOptional.get())
            Users user = userOptional.get();

            // Tạo reset token
            //Sử dụng UUID để tạo một chuỗi token ngẫu nhiên duy nhất.
            String token = UUID.randomUUID().toString();
            //Gán token này vào thuộc tính resetToken của đối tượng user.
            user.setResetToken(token);

            // Đặt thời gian hết hạn token
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // Gửi email
            emailService.sendEmail(user.getEmail(), "Reset Password", "Token để đặt lại mật khẩu: " + token);

            return "Email đặt lại mật khẩu đã được gửi!";
        } else {
            throw new RuntimeException("Email không tồn tại!");
        }
    }

    public String resetPassword(String token, String newPassword) {
        Optional<Users> userOptional = userRepository.findByResetToken(token);
        if (userOptional.isPresent()) {
            Users user = userOptional.get();

            if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
                throw new RuntimeException("Token đã hết hạn và đã bị xóa!");
            }

            // Mã hóa mật khẩu
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(newPassword);

            // Cập nhật mật khẩu và xóa token
            user.setPassword(encodedPassword);
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            return "Mật khẩu đã được đặt lại thành công!";
        } else {
            throw new RuntimeException("Token không hợp lệ!");
        }
    }
}

