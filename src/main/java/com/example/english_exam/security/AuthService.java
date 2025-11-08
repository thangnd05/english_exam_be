// src/main/java/com/example/english_exam/services/AuthService.java
package com.example.english_exam.security;

import com.example.english_exam.dto.auth.UserTokenInfo;
import com.example.english_exam.dto.request.RegisterRequest;
import com.example.english_exam.dto.response.AuthResponse;
import com.example.english_exam.models.EmailVerification;
import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.EmailVerificationRepository;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.config.CustomUserDetailsService;
import com.example.english_exam.services.EmailVerificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationRepository emailVerificationRepository; // ✅ thêm dòng này





    // src/main/java/com/example/english_exam/security/AuthService.java

    public Map<String, Object> login(String identifier, String password, HttpServletResponse response) {
        try {
            // 🧩 Xác thực username/email + password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (BadCredentialsException ex) {
            throw new RuntimeException("Thông tin đăng nhập không đúng");
        }

        // 🔍 Tìm user theo username hoặc email
        User user = userRepository.findByUserName(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 🚫 Kiểm tra xác thực email
        if (!user.getVerified()) {
            throw new RuntimeException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email để kích hoạt tài khoản.");
        }

        // 🧠 Lấy thông tin chi tiết user (phục vụ cho token)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

        // 🪙 Thêm thông tin bổ sung vào token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roleId", user.getRoleId());

        // 🔐 Sinh access token & refresh token
        String accessToken = jwtService.generateToken(userDetails, claims);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // 🍪 Lưu accessToken vào cookie (HttpOnly)
        setAccessTokenCookie(accessToken, response);

        // 🧾 Chuẩn bị dữ liệu user trả về FE
        Map<String, Object> userResponse = Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail(),
                "roleId", user.getRoleId(),
                "verified", user.getVerified()
        );

        // ✅ Trả về response cho FE
        return Map.of(
                "message", "Đăng nhập thành công",
                "refreshToken", refreshToken,
                "user", userResponse
        );
    }


    public Map<String, Object> refresh(String refreshToken, HttpServletResponse response) {
        String username = jwtService.extractUsername(refreshToken);
        if (username == null || !jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Refresh token hết hạn hoặc không hợp lệ");
        }

        User user = userRepository.findByUserName(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roleId", user.getRoleId());

        String newAccessToken = jwtService.generateToken(userDetails, claims);

        // set accessToken vào cookie HttpOnly
        String cookieValue = URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8);
        int cookieMax = (int) ((jwtService.extractClaim(newAccessToken, Claims::getExpiration).getTime() - System.currentTimeMillis()) / 1000);
        if (cookieMax <= 0) cookieMax = 3600;

        String setCookie = "accessToken=" + cookieValue +
                "; HttpOnly; Path=/; Max-Age=" + cookieMax + "; SameSite=Strict; Secure";
        response.addHeader("Set-Cookie", setCookie);

        // Chỉ trả về message
        return Map.of("message", "Cấp access token mới thành công");
    }

    public void logout(HttpServletResponse response) {
        String delCookie = "accessToken=; HttpOnly; Path=/; Max-Age=0; SameSite=Strict; Secure";
        response.addHeader("Set-Cookie", delCookie);
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        String cookieValue = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        int cookieMax = (int) ((jwtService.extractClaim(accessToken, Claims::getExpiration).getTime() - System.currentTimeMillis()) / 1000);
        if (cookieMax <= 0) cookieMax = 3600;

        String setCookie = "accessToken=" + cookieValue +
                "; HttpOnly; Path=/; Max-Age=" + cookieMax +
                "; SameSite=None" +  // ✅ Cho phép cross-site
                "; Secure";          // ⚠️ Giữ true nếu HTTPS, false nếu localhost
        response.addHeader("Set-Cookie", setCookie);
    }


    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        // 1️⃣ Kiểm tra username
        if (userRepository.findByUserName(request.getUserName()).isPresent())
            throw new RuntimeException("Tên đăng nhập đã tồn tại");

        // 2️⃣ Kiểm tra email
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            User existUser = existing.get();
            if (existUser.getVerified()) {
                throw new RuntimeException("Email đã được sử dụng");
            } else {
                // 🧹 Xóa user chưa xác thực
                emailVerificationRepository.deleteByUserId(existUser.getUserId());
                userRepository.delete(existUser);
                System.out.println("🧹 Xóa user chưa xác thực để đăng ký lại: " + existUser.getEmail());
            }
        }

        // 3️⃣ Tạo user mới
        User user = new User();
        user.setUserName(request.getUserName());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setVerified(false);

        Role userRole = roleRepository.findByRoleName("USER");
        user.setRoleId(userRole.getRoleId());
        userRepository.save(user);

        // 4️⃣ Gửi mail xác thực
        try {
            emailVerificationService.createVerification(user);
        } catch (Exception e) {
            // Gửi mail lỗi → rollback luôn
            userRepository.delete(user);
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng kiểm tra địa chỉ email.");
        }

        return Map.of("message", "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
    }


    public UserTokenInfo getCurrentUserInfo(HttpServletRequest request) {
        try {
            // 🧩 1. Trích xuất toàn bộ claims từ JWT
            Claims claims = jwtService.extractAllClaimsFromRequest(request);
            Long userId = null;
            Long roleId = null;

            Object userIdObj = claims.get("userId");
            Object roleIdObj = claims.get("roleId");

            if (userIdObj != null)
                userId = Long.parseLong(userIdObj.toString());
            if (roleIdObj != null)
                roleId = Long.parseLong(roleIdObj.toString());

            // 🧩 2. Fallback nếu token không chứa userId / roleId
            if (userId == null || roleId == null) {
                String username = claims.getSubject();

                // 🔒 an toàn hơn — tách 2 query riêng
                var user = userRepository.findByUserName(username)
                        .or(() -> userRepository.findByEmail(username))
                        .orElseThrow(() -> new RuntimeException("User not found"));

                userId = user.getUserId();
                roleId = user.getRoleId();
            }

            // 🧩 3. Trả ra DTO chứa thông tin user từ token
            return new UserTokenInfo(userId, roleId);

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to extract user info: " + e.getMessage());
        }
    }





}
