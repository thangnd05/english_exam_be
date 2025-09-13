package com.example.english_exam.controllers;

import com.example.english_exam.models.Passage;
import com.example.english_exam.services.PassageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passages")
public class PassageController {

    private final PassageService passageService;

    public PassageController(PassageService passageService) {
        this.passageService = passageService;
    }

    @GetMapping
    public ResponseEntity<List<Passage>> getAllPassages() {
        return ResponseEntity.ok(passageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Passage> getPassageById(@PathVariable Long id) {
        return passageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Passage> createPassage(@RequestBody Passage passage) {
        return ResponseEntity.ok(passageService.save(passage));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Passage> updatePassage(@PathVariable Long id, @RequestBody Passage updatedPassage) {
        return passageService.findById(id)
                .map(existing -> {
                    updatedPassage.setPassageId(id);
                    return ResponseEntity.ok(passageService.save(updatedPassage));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePassage(@PathVariable Long id) {
        return passageService.findById(id)
                .map(existing -> {
                    passageService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
