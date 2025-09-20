package com.example.english_exam.controllers;

import com.example.english_exam.models.PracticeOption;
import com.example.english_exam.services.LearningVoca.PracticeOptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-options")
public class PracticeOptionController {

    private final PracticeOptionService service;

    public PracticeOptionController(PracticeOptionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PracticeOption>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PracticeOption> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PracticeOption> create(@RequestBody PracticeOption option) {
        return ResponseEntity.ok(service.save(option));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticeOption> update(@PathVariable Long id, @RequestBody PracticeOption option) {
        option.setId(id);
        return ResponseEntity.ok(service.save(option));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.deleteById(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
