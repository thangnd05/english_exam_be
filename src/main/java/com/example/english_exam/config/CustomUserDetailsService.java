package com.example.english_exam.config;

import com.example.english_exam.models.Role;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@Getter
@Setter
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder; // Thêm dòng này

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository,@Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public void processOAuthPostLogin(String email, String name) {
        // 1. Kiểm tra xem Email này đã tồn tại trong DB chưa
        Optional<User> existUser = userRepository.findByEmail(email);

        if (existUser.isEmpty()) {
            // 2. Nếu CHƯA CÓ -> Tạo User mới
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);

            // Tạo username từ email (Cắt bỏ phần @gmail.com)
            String autoUsername = email.split("@")[0];
            // Đề phòng username bị trùng, bạn có thể thêm số ngẫu nhiên hoặc kiểm tra DB
            if (userRepository.findByUserName(autoUsername).isPresent()) {
                autoUsername = autoUsername + "_" + System.currentTimeMillis();
            }
            newUser.setUserName(autoUsername);

            // Vì đăng nhập qua Google, mật khẩu không dùng đến nhưng vẫn cần set (vì cột pass thường NOT NULL)
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            newUser.setVerified(true); // Mặc định tin tưởng Google nên cho verified luôn

            // Gán Role mặc định cho User mới (Ví dụ: ROLE_USER)
            // Bạn cần đảm bảo trong bảng Roles đã có sẵn bản ghi "USER"
            // Tìm Role có tên là "USER"
            Role userRole = roleRepository.findByRoleName("USER");
            if (userRole == null) {
                // Nếu DB chưa có role USER, hãy tạo hoặc báo lỗi
                throw new RuntimeException("Lỗi: Role 'USER' không tồn tại trong Database!");
            }

            newUser.setRoleId(userRole.getRoleId());

            userRepository.save(newUser);

            System.out.println("--- Đã tạo tài khoản mới cho user: " + email);
        } else {
            // 3. Nếu ĐÃ CÓ -> Cập nhật thông tin (nếu cần)
            User user = existUser.get();
            // Ví dụ: Cập nhật tên mới nhất từ Google
            user.setFullName(name);
            userRepository.save(user);
            System.out.println("--- User đã tồn tại, chỉ cập nhật thông tin: " + email);
        }
    }

}
