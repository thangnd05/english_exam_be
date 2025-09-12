package com.example.english_exam.controllers;

import com.example.english_exam.models.ExamType;
import com.example.english_exam.services.ExamTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-types")
public class ExamTypeController {
    private final ExamTypeService examTypeService;

    public ExamTypeController(ExamTypeService examTypeService) {
        this.examTypeService = examTypeService;
    }

    // Lấy tất cả exam types
    @GetMapping
    public ResponseEntity<List<ExamType>> getAllExamTypes() {
        return ResponseEntity.ok(examTypeService.findAll());
    }

    // Lấy exam type theo id
    @GetMapping("/{id}")
    public ResponseEntity<ExamType> getExamTypeById(@PathVariable Long id) {
        return examTypeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới exam type
    @PostMapping
    public ResponseEntity<ExamType> createExamType(@RequestBody ExamType examType) {
        return ResponseEntity.ok(examTypeService.save(examType));
    }

    // Cập nhật exam type
    @PutMapping("/{id}")
    public ResponseEntity<ExamType> updateExamType(@PathVariable Long id, @RequestBody ExamType updatedExamType) {
        return examTypeService.findById(id)
                .map(existing -> {
                    updatedExamType.setExamTypeId(id);
                    return ResponseEntity.ok(examTypeService.save(updatedExamType));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa exam type
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExamType(@PathVariable Long id) {
        return examTypeService.findById(id)
                .map(existing -> {
                    examTypeService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
