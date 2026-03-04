package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.VocabularyRequest;
import com.example.english_exam.dto.response.VocabularyResponse;
import com.example.english_exam.models.Vocabulary;
import com.example.english_exam.repositories.VocabularyAlbumRepository;
import com.example.english_exam.services.LearningVoca.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vocabularies")
@AllArgsConstructor
public class VocabularyController {
    private final VocabularyService service;
    private final VocabularyAlbumRepository albumRepository;

    @GetMapping
    public ResponseEntity<List<VocabularyResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<VocabularyResponse> create(@RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(service.createVocabulary(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VocabularyResponse> update(
            @PathVariable Long id,
            @RequestBody VocabularyRequest request
    ) {
        try {
            VocabularyResponse response = service.updateVocabulary(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<VocabularyResponse>> getVocabulariesByAlbumId(
            @PathVariable Long albumId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(service.findAllByAlbumId(albumId, request));
    }
}
