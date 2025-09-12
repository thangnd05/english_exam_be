package com.example.english_exam.controllers;

import com.example.english_exam.models.ExamPart;
import com.example.english_exam.services.ExamPartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-parts")
public class ExamPartController {
    private final ExamPartService examPartService;

    public ExamPartController(ExamPartService examPartService) {
        this.examPartService = examPartService;
    }

    // Lấy tất cả exam parts
    @GetMapping
    public ResponseEntity<List<ExamPart>> getAllExamParts() {
        return ResponseEntity.ok(examPartService.findAll());
    }

    // Lấy tất cả exam parts theo examTypeId
    @GetMapping("/by-exam-type/{examTypeId}")
    public ResponseEntity<List<ExamPart>> getExamPartsByExamType(@PathVariable Long examTypeId) {
        return ResponseEntity.ok(examPartService.findByExamTypeId(examTypeId));
    }

    // Lấy exam part theo id
    @GetMapping("/{id}")
    public ResponseEntity<ExamPart> getExamPartById(@PathVariable Long id) {
        return examPartService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới exam part
    @PostMapping
    public ResponseEntity<ExamPart> createExamPart(@RequestBody ExamPart examPart) {
        return ResponseEntity.ok(examPartService.save(examPart));
    }

    // Cập nhật exam part
    @PutMapping("/{id}")
    public ResponseEntity<ExamPart> updateExamPart(@PathVariable Long id, @RequestBody ExamPart updatedExamPart) {
        return examPartService.findById(id)
                .map(existing -> {
                    updatedExamPart.setExamPartId(id);
                    return ResponseEntity.ok(examPartService.save(updatedExamPart));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa exam part
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExamPart(@PathVariable Long id) {
        return examPartService.findById(id)
                .map(existing -> {
                    examPartService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
