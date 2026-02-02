package com.example.english_exam.controllers;

import com.example.english_exam.models.User;
import com.example.english_exam.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Lấy danh sách user
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    // Lấy user theo id
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới user
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody User user
    ) {
        return ResponseEntity.ok(userService.createUser(user));
    }


    // Cập nhật user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestPart("user") User updatedUser,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) throws IOException {

        return ResponseEntity.ok(userService.updateUser(id, updatedUser, avatar));
    }

    // Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(existing -> {
                    userService.deleteUser(id);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }
}
