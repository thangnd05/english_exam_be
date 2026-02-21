package com.example.english_exam.controllers;

import com.example.english_exam.models.ScoringConversion;
import com.example.english_exam.services.ExamAndTest.ScoringConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scoring-conversions")
public class ScoringConversionController {

    private final ScoringConversionService scoringConversionService;

    public ScoringConversionController(ScoringConversionService scoringConversionService) {
        this.scoringConversionService = scoringConversionService;
    }

    @GetMapping
    public ResponseEntity<List<ScoringConversion>> getAll() {
        return ResponseEntity.ok(scoringConversionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScoringConversion> getById(@PathVariable Long id) {
        return scoringConversionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ScoringConversion> create(@RequestBody ScoringConversion conversion) {
        return ResponseEntity.ok(scoringConversionService.save(conversion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScoringConversion> update(@PathVariable Long id, @RequestBody ScoringConversion conversion) {
        conversion.setConversionId(id);
        return ResponseEntity.ok(scoringConversionService.save(conversion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return scoringConversionService.findById(id)
                .map(existing -> {
                    scoringConversionService.delete(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
