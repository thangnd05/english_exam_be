package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.SkillRequest;
import com.example.english_exam.dto.response.SkillResponse;
import com.example.english_exam.services.ExamAndTest.SkillService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@AllArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAll() {
        return ResponseEntity.ok(skillService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(skillService.findById(id));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<SkillResponse> create(@RequestBody SkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> update(@PathVariable Long id, @RequestBody SkillRequest request) {
        try {
            return ResponseEntity.ok(skillService.update(id, request));
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
            skillService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
}
