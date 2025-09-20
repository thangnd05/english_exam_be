package com.example.english_exam.controllers;

import com.example.english_exam.models.PracticeAnswer;
import com.example.english_exam.services.LearningVoca.PracticeAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-answers")
public class PracticeAnswerController {

    private final PracticeAnswerService service;

    public PracticeAnswerController(PracticeAnswerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PracticeAnswer>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PracticeAnswer> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PracticeAnswer> create(@RequestBody PracticeAnswer answer) {
        return ResponseEntity.ok(service.save(answer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticeAnswer> update(@PathVariable Long id, @RequestBody PracticeAnswer answer) {
        answer.setId(id);
        return ResponseEntity.ok(service.save(answer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.deleteById(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
