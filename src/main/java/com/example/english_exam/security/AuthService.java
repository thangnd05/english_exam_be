// src/main/java/com/example/english_exam/services/AuthService.java
package com.example.english_exam.security;

import com.example.english_exam.dto.auth.UserTokenInfo;
import com.example.english_exam.dto.request.RegisterRequest;
import com.example.english_exam.dto.response.AuthResponse;
import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.config.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, CustomUserDetailsService customUserDetailsService, JwtService jwtService, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    // src/main/java/com/example/english_exam/security/AuthService.java

    public Map<String, Object> login(String identifier, String password, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (BadCredentialsException ex) {
            throw new RuntimeException("Thông tin đăng nhập không đúng");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roleId", user.getRoleId());

        String accessToken = jwtService.generateToken(userDetails, claims);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        setAccessTokenCookie(accessToken, response);

        // --- SỬA Ở ĐÂY ---
        // Tạo một Map chứa thông tin user để trả về
        Map<String, Object> userResponse = Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail(),
                "roleId", user.getRoleId() // Có thể thêm role name nếu cần
        );

        // Trả về cả message, refreshToken và object user
        return Map.of(
                "message", "Đăng nhập thành công",
                "refreshToken", refreshToken,
                "user", userResponse // Thêm object user vào response
        );
    }

    // hiển thị token khi refresh để test
//    public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
//        String username = jwtService.extractUsername(refreshToken);
//        if (username == null || !jwtService.isRefreshToken(refreshToken)) {
//            throw new RuntimeException("Refresh token không hợp lệ");
//        }
//
//        User user = userRepository.findByUserNameOrEmail(username, username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
//                user.getEmail(), user.getPassword(), new java.util.ArrayList<>()
//        );
//
//        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
//            throw new RuntimeException("Refresh token hết hạn hoặc không hợp lệ");
//        }
//
//        // Sinh access token mới
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("userId", user.getUserId());
//        claims.put("roleId", user.getRoleId());
//
//        String newAccessToken = jwtService.generateToken(userDetails, claims);
//
//        // Set cookie accessToken mới
//        String cookieValue = URLEncoder.encode(newAccessToken, StandardCharsets.UTF_8);
//        int cookieMax = (int) ((jwtService.extractClaim(newAccessToken, c -> c.getExpiration()).getTime()
//                - System.currentTimeMillis()) / 1000);
//        if (cookieMax <= 0) cookieMax = 3600;
//
//        String setCookie = "accessToken=" + cookieValue +
//                "; HttpOnly; Path=/; Max-Age=" + cookieMax + "; SameSite=Strict; Secure";
//        response.addHeader("Set-Cookie", setCookie);
//
//        // Trả về DTO cho client
//        return new AuthResponse(newAccessToken, refreshToken, "Cấp access token mới thành công");
//    }

    public Map<String, Object> refresh(String refreshToken, HttpServletResponse response) {
        String username = jwtService.extractUsername(refreshToken);
        if (username == null || !jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Refresh token hết hạn hoặc không hợp lệ");
        }

        User user = userRepository.findByUserNameOrEmail(username, username)
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


    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.findByUserNameOrEmail(request.getUserName(), request.getEmail()).isPresent()) {
            throw new RuntimeException("Username hoặc email đã tồn tại");
        }

        User user = new User();
        user.setUserName(request.getUserName());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByRoleName("USER");

        user.setRoleId(userRole.getRoleId()); // Sử dụng ID từ role tìm được


        userRepository.save(user);

        return Map.of("message", "Đăng ký thành công");
    }


    public UserTokenInfo getCurrentUserInfo(HttpServletRequest request) {
        try {
            Claims claims = jwtService.extractAllClaimsFromRequest(request);
            Long userId = null;
            Long roleId = null;

            Object userIdObj = claims.get("userId");
            Object roleIdObj = claims.get("roleId");

            if (userIdObj != null)
                userId = Long.parseLong(userIdObj.toString());
            if (roleIdObj != null)
                roleId = Long.parseLong(roleIdObj.toString());

            // fallback nếu token thiếu thông tin
            if (userId == null) {
                String username = claims.getSubject();
                var user = userRepository.findByUserNameOrEmail(username, username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                userId = user.getUserId();
                roleId = user.getRoleId();
            }

            return new UserTokenInfo(userId, roleId);
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to extract user info: " + e.getMessage());
        }
    }




}
