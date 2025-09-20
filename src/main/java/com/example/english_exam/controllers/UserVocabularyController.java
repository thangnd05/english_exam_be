package com.example.english_exam.controllers;

import com.example.english_exam.models.UserVocabulary;
import com.example.english_exam.services.LearningVoca.UserVocabularyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-vocabulary")
public class UserVocabularyController {
    private final UserVocabularyService service;

    public UserVocabularyController(UserVocabularyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UserVocabulary>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserVocabulary> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserVocabulary> create(@RequestBody UserVocabulary uv) {
        return ResponseEntity.ok(service.save(uv));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserVocabulary> update(@PathVariable Long id, @RequestBody UserVocabulary uv) {
        uv.setId(id);
        return ResponseEntity.ok(service.save(uv));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
