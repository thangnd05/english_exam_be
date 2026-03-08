package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.ExamPartRequest;
import com.example.english_exam.dto.response.ExamPartResponse;
import com.example.english_exam.services.ExamAndTest.ExamPartService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-parts")
@AllArgsConstructor
public class ExamPartController {

    private final ExamPartService examPartService;

    @GetMapping
    public ResponseEntity<List<ExamPartResponse>> getAllExamParts() {
        return ResponseEntity.ok(examPartService.findAll());
    }

    @GetMapping("/by-exam-type/{examTypeId}")
    public ResponseEntity<List<ExamPartResponse>> getExamPartsByExamType(@PathVariable Long examTypeId) {
        return ResponseEntity.ok(examPartService.findByExamTypeId(examTypeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamPartResponse> getExamPartById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(examPartService.findById(id));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<ExamPartResponse> createExamPart(@RequestBody ExamPartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examPartService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamPartResponse> updateExamPart(@PathVariable Long id, @RequestBody ExamPartRequest request) {
        try {
            return ResponseEntity.ok(examPartService.update(id, request));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExamPart(@PathVariable Long id) {
        try {
            examPartService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
}
