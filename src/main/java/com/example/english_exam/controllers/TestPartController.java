package com.example.english_exam.controllers;

import com.example.english_exam.models.TestPart;
import com.example.english_exam.services.ExamAndTest.TestPartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-parts")
public class TestPartController {
    private final TestPartService testPartService;

    public TestPartController(TestPartService testPartService) {
        this.testPartService = testPartService;
    }

    @GetMapping
    public ResponseEntity<List<TestPart>> getAllTestParts() {
        return ResponseEntity.ok(testPartService.findAll());
    }

    // Lấy test parts theo testId
    @GetMapping("/by-test/{testId}")
    public ResponseEntity<List<TestPart>> getTestPartsByTestId(@PathVariable Long testId) {
        return ResponseEntity.ok(testPartService.findByTestId(testId));
    }

    // Lấy test part theo id
    @GetMapping("/{id}")
    public ResponseEntity<TestPart> getTestPartById(@PathVariable Long id) {
        return testPartService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới test part
    @PostMapping
    public ResponseEntity<TestPart> createTestPart(@RequestBody TestPart testPart) {
        return ResponseEntity.ok(testPartService.save(testPart));
    }

    // Cập nhật test part
    @PutMapping("/{id}")
    public ResponseEntity<TestPart> updateTestPart(@PathVariable Long id, @RequestBody TestPart updatedTestPart) {
        return testPartService.findById(id)
                .map(existing -> {
                    updatedTestPart.setTestPartId(id);
                    return ResponseEntity.ok(testPartService.save(updatedTestPart));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa test part
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTestPart(@PathVariable Long id) {
        return testPartService.findById(id)
                .map(existing -> {
                    testPartService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
