package com.example.test.LoginRegister;

import com.example.test.Dto.LoginDto;
import com.example.test.models.Users;
import com.example.test.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class Login {

    private final UserService userService;

    @Autowired
    public Login(UserService userService) {
        this.userService = userService;
    }



    @PostMapping("/login")
    public ResponseEntity<Users> login(@Valid @RequestBody LoginDto loginDto, HttpSession session) {
        return userService.loginUser(loginDto,session);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        return userService.logout(session);
    }

    @GetMapping("/check-session")
    public ResponseEntity<Users> checkSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            Optional<Users> user = userService.getUserId(userId);
            return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Hoặc 404 nếu không tìm thấy user
        }
        // Không đăng nhập nhưng không báo lỗi
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

}

