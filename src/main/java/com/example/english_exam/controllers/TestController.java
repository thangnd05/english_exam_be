package com.example.english_exam.controllers;

import com.example.english_exam.models.Test;
import com.example.english_exam.services.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    // Lấy tất cả tests
    @GetMapping
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests()); // 200 OK
    }

    // Lấy test theo id
    @GetMapping("/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable Long id) {
        return testService.getTestById(id)
                .map(ResponseEntity::ok)   // 200 OK
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    // Tạo test mới
    @PostMapping
    public ResponseEntity<Test> createTest(@RequestBody Test test) {
        Test savedTest = testService.save(test);
        return ResponseEntity.created(URI.create("/api/tests/" + savedTest.getTestId()))
                .body(savedTest); // 201 Created
    }

    // Cập nhật test
    @PutMapping("/{id}")
    public ResponseEntity<Test> updateTest(@PathVariable Long id, @RequestBody Test updatedTest) {
        return testService.getTestById(id)
                .map(existing -> {
                    updatedTest.setTestId(id);
                    return ResponseEntity.ok(testService.save(updatedTest)); // 200 OK
                })
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    // Xoá test
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable Long id) {
        return testService.getTestById(id)
                .map(existing -> {
                    testService.deleteTest(id);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

}
