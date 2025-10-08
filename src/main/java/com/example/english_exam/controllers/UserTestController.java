package com.example.english_exam.controllers;

import com.example.english_exam.dto.response.UserTestResponse;
import com.example.english_exam.models.UserTest;
import com.example.english_exam.security.AuthService;
import com.example.english_exam.services.ExamAndTest.UserTestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user-tests")
@AllArgsConstructor
public class UserTestController {

    private final UserTestService userTestService;
    private final AuthService authService;

    // ✅ Lấy tất cả user test
    @GetMapping
    public ResponseEntity<List<UserTest>> getAll() {
        return ResponseEntity.ok(userTestService.findAll());
    }

    // ✅ Lấy theo ID
    @GetMapping("/{userTestId}")
    public ResponseEntity<UserTestResponse> getUserTestById(@PathVariable Long userTestId) {
        return userTestService.findById(userTestId)
                .map(ut -> ResponseEntity.ok(
                        UserTestResponse.builder()
                                .userTestId(ut.getUserTestId())
                                .userId(ut.getUserId())
                                .testId(ut.getTestId())
                                .startedAt(ut.getStartedAt())
                                .finishedAt(ut.getFinishedAt())
                                .totalScore(ut.getTotalScore())
                                .status(ut.getStatus().name())
                                .build()
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Lấy theo userId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserTest>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userTestService.findByUserId(userId));
    }

    // ✅ Lấy theo testId
    @GetMapping("/test/{testId}")
    public ResponseEntity<List<UserTest>> getByTest(@PathVariable Long testId) {
        return ResponseEntity.ok(userTestService.findByTestId(testId));
    }

    // ✅ Tạo hoặc bắt đầu bài test mới
    @PostMapping
    public ResponseEntity<?> startUserTest(@RequestBody Map<String, Long> request,
                                           HttpServletRequest httpRequest) {
        try {
            Long testId = request.get("testId");

            if (testId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing testId"));
            }

            // ✅ Lấy userId từ JWT token
            Long userId = authService.getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            // ✅ Tạo hoặc tái sử dụng UserTest
            UserTest userTest = userTestService.startUserTest(testId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bắt đầu làm bài thành công");
            response.put("userTestId", userTest.getUserTestId());
            response.put("status", userTest.getStatus() != null ? userTest.getStatus().name() : "UNKNOWN");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Cập nhật UserTest
    @PutMapping("/{id}")
    public ResponseEntity<UserTest> update(@PathVariable Long id, @RequestBody UserTest userTest) {
        userTest.setUserTestId(id);
        return ResponseEntity.ok(userTestService.save(userTest));
    }

    // ✅ Xóa UserTest
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return userTestService.findById(id)
                .map(existing -> {
                    userTestService.delete(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Nộp bài thi
    @PostMapping("/{userTestId}/submit")
    public ResponseEntity<UserTest> submitTest(@PathVariable Long userTestId) {
        try {
            UserTest submittedTest = userTestService.submitTest(userTestId);
            return ResponseEntity.ok(submittedTest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Kiểm tra có đang làm dở không
    @GetMapping("/check-active")
    public ResponseEntity<?> checkActiveUserTest(
            @RequestParam Long testId,
            @RequestParam Long userId
    ) {
        try {
            Optional<UserTest> active = userTestService.findActiveUserTest(userId, testId);

            Map<String, Object> response = new HashMap<>();
            if (active.isPresent()) {
                UserTest userTest = active.get();
                response.put("userTestId", userTest.getUserTestId());
                response.put("status", userTest.getStatus() != null ? userTest.getStatus().name() : "UNKNOWN");
            } else {
                response.put("userTestId", null);
                response.put("status", "NONE");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/by-user/{userId}/by-test/{testId}")
    public ResponseEntity<List<UserTestResponse>> getAttempts(
            @PathVariable Long userId,
            @PathVariable Long testId) {

        List<UserTestResponse> res = userTestService.getAttemptsByUserAndTest(userId, testId);
        return ResponseEntity.ok(res);
    }
}
