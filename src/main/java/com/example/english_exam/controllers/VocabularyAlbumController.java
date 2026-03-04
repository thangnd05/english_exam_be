package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.VocabularyAlbumRequest;
import com.example.english_exam.dto.response.VocabularyAlbumResponse;
import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.services.LearningVoca.VocabularyAlbumService;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/vocabulary-albums")
public class VocabularyAlbumController {
    private final VocabularyAlbumService service;
    private final AuthUtils authUtils;

    @GetMapping
    public ResponseEntity<List<VocabularyAlbumResponse>> getAll() {
        List<VocabularyAlbumResponse> responses = service.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularyAlbumResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VocabularyAlbumResponse> create(
            @RequestBody VocabularyAlbumRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = authUtils.getUserId(httpRequest);
        VocabularyAlbum album = toEntity(request);
        album.setUserId(userId);
        VocabularyAlbum saved = service.save(album);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VocabularyAlbumResponse> update(
            @PathVariable Long id,
            @RequestBody VocabularyAlbumRequest request
    ) {
        return service.findById(id)
                .map(album -> {
                    album.setName(request.getName());
                    album.setDescription(request.getDescription());
                    return ResponseEntity.ok(toResponse(service.save(album)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/my-albums")
    public ResponseEntity<List<VocabularyAlbumResponse>> getMyAlbums(HttpServletRequest request) {
        List<VocabularyAlbumResponse> responses = service.findAllByUserId(request).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private VocabularyAlbumResponse toResponse(VocabularyAlbum album) {
        VocabularyAlbumResponse response = new VocabularyAlbumResponse();
        response.setAlbumId(album.getAlbumId());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setUserId(album.getUserId());
        response.setCreatedAt(album.getCreatedAt());
        return response;
    }

    private VocabularyAlbum toEntity(VocabularyAlbumRequest request) {
        VocabularyAlbum album = new VocabularyAlbum();
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        return album;
    }
}
