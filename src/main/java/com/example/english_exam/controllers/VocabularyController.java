package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.VocabularyRequest;
import com.example.english_exam.dto.response.VocabularyResponse;
import com.example.english_exam.models.Vocabulary;
import com.example.english_exam.services.LearningVoca.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vocabularies")
@AllArgsConstructor
public class VocabularyController {
    private final VocabularyService service;


    @GetMapping
    public ResponseEntity<List<Vocabulary>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vocabulary> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 👉 Dùng request/response thay vì entity trực tiếp
    @PostMapping
    public ResponseEntity<VocabularyResponse> create(@RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(service.createVocabulary(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vocabulary> update(@PathVariable Long id, @RequestBody Vocabulary vocab) {
        vocab.setVocabId(id);
        return ResponseEntity.ok(service.save(vocab));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<Vocabulary>> getVocabulariesByAlbumId(
            @PathVariable Long albumId,
            HttpServletRequest request
    ) {
        List<Vocabulary> vocabularies = service.findAllByAlbumId(albumId, request);
        return ResponseEntity.ok(vocabularies);
    }

}
