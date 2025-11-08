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
    private final RoleRepository roleRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // 🔍 Tìm user theo username hoặc email
        User user = userRepository.findByUserName(input)
                .or(() -> userRepository.findByEmail(input))
                .orElseThrow(() ->
                        new UsernameNotFoundException("Không tìm thấy người dùng với: " + input));

        // 🚫 Nếu user chưa xác thực email → không cho login
        if (!user.getVerified()) {
            throw new UsernameNotFoundException("Tài khoản chưa được xác thực qua email.");
        }

        // 🔑 Lấy role name
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role ID: " + user.getRoleId()));

        String roleName = "ROLE_" + role.getRoleName().toUpperCase();

        // ✅ Trả về đối tượng UserDetails cho Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),          // dùng email để login
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
