package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.LoginRequest;
import com.example.english_exam.dto.request.RegisterRequest;
import com.example.english_exam.models.EmailVerification;
import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.EmailVerificationRepository;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.security.AuthService;
import com.example.english_exam.services.EmailVerificationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository  userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationService emailVerificationService;



    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(request.getIdentifier(), request.getPassword(), response));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (RuntimeException e) {
            // Trả về lỗi 400 cùng message để FE đọc được
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUserName(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));


        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        Map<String, Object> userInfo = Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail(),
                "role", role.getRoleName()
        );

        return ResponseEntity.ok(userInfo);
    }



    // AuthController.java
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body, HttpServletResponse response) {
//        String refreshToken = body.get("refreshToken");
//        if (refreshToken == null || refreshToken.isBlank()) {
//            return ResponseEntity.badRequest().body(Map.of("message", "refreshToken required"));
//        }
//        return ResponseEntity.ok(authService.refresh(refreshToken, response));
//    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(authService.refresh(refreshToken, response));
    }



    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of("message", "Đã logout"));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        return emailVerificationService.verifyToken(token);
    }

}
