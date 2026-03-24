package com.example.english_exam.security;

import com.example.english_exam.dto.auth.UserTokenInfo;
import com.example.english_exam.dto.request.ChangePasswordRequest;
import com.example.english_exam.dto.request.ForgotPasswordRequest;
import com.example.english_exam.dto.request.RegisterRequest;
import com.example.english_exam.dto.request.ResetPasswordRequest;
import com.example.english_exam.dto.response.AuthMessageResponse;
import com.example.english_exam.dto.response.UserResponse;
import com.example.english_exam.models.PasswordResetToken;
import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.EmailVerificationRepository;
import com.example.english_exam.repositories.PasswordResetTokenRepository;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.config.CustomUserDetailsService;
import com.example.english_exam.services.EmailVerificationService;
import com.example.english_exam.util.EmailUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailUtil emailUtil;





    // src/main/java/com/example/english_exam/security/AuthService.java

    public UserResponse login(String identifier, String password, HttpServletResponse response) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );

        User user = userRepository.findByUserName(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!user.getVerified()) {
            throw new RuntimeException("Tài khoản chưa xác thực email");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roleId", user.getRoleId());

        String accessToken = jwtService.generateToken(userDetails, claims);

        // ✅ set cookie
        setAccessTokenCookie(accessToken, response);

        // ✅ Trả DTO user phẳng
        return new UserResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getRoleId(),
                user.getAvatarUrl()
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
        // 1. Xóa accessToken của bạn
        String delAccessToken = "accessToken=; HttpOnly; Path=/; Max-Age=0; SameSite=Secure";

        // 2. Xóa JSESSIONID của Spring Security
        String delJSession = "JSESSIONID=; HttpOnly; Path=/; Max-Age=0; SameSite=Lax";

        response.addHeader("Set-Cookie", delAccessToken);
        response.addHeader("Set-Cookie", delJSession);
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

        // ✅ Avatar mặc định theo username (Google style)
        String defaultAvatar =
                "https://ui-avatars.com/api/?name="
                        + request.getUserName()
                        + "&background=random&color=fff";

        user.setAvatarUrl(defaultAvatar);

        // Role USER
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

    public UserResponse me(HttpServletRequest request) {

        Claims claims = jwtService.extractAllClaimsFromRequest(request);
        Long userId = Long.parseLong(claims.get("userId").toString());

        User user = userRepository.findById(userId)
                .orElseThrow();

        return new UserResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getRoleId(),
                user.getAvatarUrl()
        );
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

    public AuthMessageResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return new AuthMessageResponse("Nếu email tồn tại, chúng tôi đã gửi liên kết đặt lại mật khẩu.");
        }

        User user = userOptional.get();
        passwordResetTokenRepository.deleteByUserId(user.getUserId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getUserId());
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        emailUtil.sendResetPasswordEmail(user.getEmail(), token);
        return new AuthMessageResponse("Nếu email tồn tại, chúng tôi đã gửi liên kết đặt lại mật khẩu.");
    }

    public AuthMessageResponse resetPassword(ResetPasswordRequest request) {
        validateNewPassword(request.getNewPassword(), request.getConfirmNewPassword());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token đặt lại mật khẩu không hợp lệ"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new RuntimeException("Token đã được sử dụng");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return new AuthMessageResponse("Đặt lại mật khẩu thành công");
    }

    public AuthMessageResponse changePassword(ChangePasswordRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        validateNewPassword(request.getNewPassword(), request.getConfirmNewPassword());

        Long userId = getCurrentUserInfo(httpRequest).getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logout(httpResponse);
        return new AuthMessageResponse("Đổi mật khẩu thành công, vui lòng đăng nhập lại");
    }

    private void validateNewPassword(String newPassword, String confirmNewPassword) {
        if (!Objects.equals(newPassword, confirmNewPassword)) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
    }





}