package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.VocabularyRequest;
import com.example.english_exam.dto.request.VocabularyUpdateRequest;
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
        List<VocabularyResponse> responses = service.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularyResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VocabularyResponse> create(@RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(service.createVocabulary(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VocabularyResponse> update(
            @PathVariable Long id,
            @RequestBody VocabularyUpdateRequest request
    ) {
        return service.findById(id)
                .map(vocab -> {
                    vocab.setWord(request.getWord());
                    vocab.setMeaning(request.getMeaning());
                    vocab.setExample(request.getExample());
                    if (request.getAlbumId() != null) {
                        vocab.setAlbumId(request.getAlbumId());
                    }
                    Vocabulary saved = service.save(vocab);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<VocabularyResponse>> getVocabulariesByAlbumId(
            @PathVariable Long albumId,
            HttpServletRequest request
    ) {
        List<VocabularyResponse> responses = service.findAllByAlbumId(albumId, request).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private VocabularyResponse toResponse(Vocabulary vocab) {
        VocabularyResponse response = new VocabularyResponse();
        response.setVocabId(vocab.getVocabId());
        response.setWord(vocab.getWord());
        response.setPhonetic(vocab.getPhonetic());
        response.setMeaning(vocab.getMeaning());
        response.setExample(vocab.getExample());
        response.setAlbumId(vocab.getAlbumId());
        response.setVoiceUrl(vocab.getVoiceUrl());
        response.setCreatedAt(vocab.getCreatedAt());
        albumRepository.findById(vocab.getAlbumId()).ifPresent(album -> {
            response.setAlbumName(album.getName());
            response.setAlbumDesc(album.getDescription());
        });
        return response;
    }
}
