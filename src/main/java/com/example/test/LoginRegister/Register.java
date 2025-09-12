package com.example.test.LoginRegister;
import com.example.test.models.Users;
import com.example.test.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Register {
    private final UserService userService;
    @Autowired
    public Register(UserService userService) {
        this.userService = userService;
    }
//    @PostMapping("/register")
//    public ResponseEntity<Users> Register(@Valid @RequestBody Users user) {
//        try {
//            Users register = userService.save(user);
//            return new ResponseEntity<>(register, HttpStatus.CREATED);
//        } catch (Exception e) {
//            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
//        }
//    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Users user) {
          try {
              if (user.getPassword() == null || user.getPassword().isBlank()) {
                  return new ResponseEntity<>("Password cannot be empty", HttpStatus.BAD_REQUEST); // 400 Bad Request
              }
              Users registeredUser = userService.save(user);
              return new ResponseEntity<>(registeredUser, HttpStatus.CREATED); // 201 Created
          }catch (IllegalArgumentException e) {
        // Trả về lỗi khi email đã tồn tại
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict
    }
}


    @GetMapping("/user")
    public ResponseEntity<List<Users>> getAllUsers() {
        return userService.getAll();
    }


}
