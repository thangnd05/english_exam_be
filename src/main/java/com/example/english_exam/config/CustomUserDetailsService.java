// src/main/java/com/example/english_exam/config/CustomUserDetailsService.java
package com.example.english_exam.config;

import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Phải inject thêm RoleRepository

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // Query 1: Tìm User
        User user = userRepository.findByUserNameOrEmail(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với: " + input));

        // Lấy roleId từ user
        Long roleId = user.getRoleId();

        // Query 2: Dùng roleId để tìm Role. Thêm 1 lần truy vấn database!
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role với ID: " + roleId));

        String roleName = "ROLE_" + role.getRoleName().toUpperCase();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}