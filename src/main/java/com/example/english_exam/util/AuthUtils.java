package com.example.english_exam.util;

import com.example.english_exam.dto.auth.UserTokenInfo;
import com.example.english_exam.models.Role;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    private final AuthService authService;
    private final RoleRepository roleRepository;

    // 🟢 Lấy userId nhanh
    public Long getUserId(HttpServletRequest request) {
        return authService.getCurrentUserInfo(request).getUserId();
    }

    // 🟢 Lấy roleId nhanh
    public Long getRoleId(HttpServletRequest request) {
        return authService.getCurrentUserInfo(request).getRoleId();
    }

    // 🟢 Lấy cả user info nếu cần nhiều hơn
    public UserTokenInfo getUserInfo(HttpServletRequest request) {
        return authService.getCurrentUserInfo(request);
    }


}
