package com.example.english_exam.controllers;

import com.example.english_exam.models.UserTest;
import com.example.english_exam.services.UserTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-tests")
public class UserTestController {

    private final UserTestService userTestService;

    public UserTestController(UserTestService userTestService) {
        this.userTestService = userTestService;
    }

    @GetMapping
    public ResponseEntity<List<UserTest>> getAll() {
        return ResponseEntity.ok(userTestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserTest> getById(@PathVariable Long id) {
        return userTestService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserTest>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userTestService.findByUserId(userId));
    }

    @GetMapping("/test/{testId}")
    public ResponseEntity<List<UserTest>> getByTest(@PathVariable Long testId) {
        return ResponseEntity.ok(userTestService.findByTestId(testId));
    }

    @PostMapping
    public ResponseEntity<UserTest> create(@RequestBody UserTest userTest) {
        return ResponseEntity.ok(userTestService.save(userTest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserTest> update(@PathVariable Long id, @RequestBody UserTest userTest) {
        userTest.setUserTestId(id);
        return ResponseEntity.ok(userTestService.save(userTest));
    }

    // --- Xóa user test ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return userTestService.findById(id)
                .map(existing -> {
                    userTestService.delete(id);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    // --- Submit test ---
    @PostMapping("/{userTestId}/submit")
    public ResponseEntity<UserTest> submitTest(@PathVariable Long userTestId) {
        try {
            UserTest submittedTest = userTestService.submitTest(userTestId);
            return ResponseEntity.ok(submittedTest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build(); // đổi từ notFound sang badRequest nếu lỗi submit
        }
    }

}
