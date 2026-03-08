package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.UserVocabularyRequest;
import com.example.english_exam.dto.response.UserVocabularyResponse;
import com.example.english_exam.services.LearningVoca.UserVocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-vocabulary")
@AllArgsConstructor
public class UserVocabularyController {

    private final UserVocabularyService service;

    @GetMapping
    public ResponseEntity<List<UserVocabularyResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserVocabularyResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<UserVocabularyResponse> create(
            @RequestBody UserVocabularyRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserVocabularyResponse created = service.create(request, httpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserVocabularyResponse> update(
            @PathVariable Long id,
            @RequestBody UserVocabularyRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<String> deleteAll() {
        service.deleteAllUserVocabulary();
        return ResponseEntity.ok("Đã xóa toàn bộ dữ liệu trong bảng user_vocabulary!");
    }
}
