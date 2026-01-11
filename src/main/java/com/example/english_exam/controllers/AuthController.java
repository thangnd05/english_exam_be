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
import com.example.english_exam.security.JwtService;
import com.example.english_exam.services.EmailVerificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtService jwtService;



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
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            Claims claims = jwtService.extractAllClaimsFromRequest(request);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long userId = Long.parseLong(claims.get("userId").toString());

            User user = userRepository.findById(userId)
                    .orElseThrow();

            Role role = roleRepository.findById(user.getRoleId()).orElseThrow();

            return ResponseEntity.ok(Map.of(
                    "id", user.getUserId(),
                    "username", user.getUserName(),
                    "email", user.getEmail(),
                    "role", role.getRoleName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
