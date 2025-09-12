package com.example.english_exam.Loader;

import com.example.english_exam.models.Roles;
import com.example.english_exam.models.Users;
import com.example.english_exam.repositories.RoleRepository;
import com.example.english_exam.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DataLoader(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem role admin đã tồn tại chưa
        Roles adminRole = roleRepository.findByRoleName("ADMIN");
        if (adminRole == null) {
            adminRole = new Roles();
            adminRole.setRoleName("ADMIN");
            adminRole.setDescription("Quyền quản trị viên");
            roleRepository.save(adminRole);
        }

        // Kiểm tra user admin
        Optional<Users> adminUser = userRepository.findByUserName("WinDe");
        if(adminUser.isEmpty()){
            Users user = new Users();
            user.setUserName("WinDe");
            user.setFullName("WinDe");
            user.setEmail("winde");
            user.setPassword("123456");
            user.setRoleId(adminRole.getRoleId());
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);

        }

        System.out.println("DataLoader: Default role and admin user created (if not exist).");
    }
}
