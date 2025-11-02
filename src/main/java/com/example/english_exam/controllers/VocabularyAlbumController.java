package com.example.english_exam.controllers;

import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.services.LearningVoca.VocabularyAlbumService;
import com.example.english_exam.services.LearningVoca.VocabularyService;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/vocabulary-albums")
public class VocabularyAlbumController {
    private final VocabularyAlbumService service;
    private final AuthUtils authUtils;


    @GetMapping
    public ResponseEntity<List<VocabularyAlbum>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VocabularyAlbum> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VocabularyAlbum> create(
            @RequestBody VocabularyAlbum album,
            HttpServletRequest request
    ) {
        Long userId = authUtils.getUserId(request);
        album.setUserId(userId);
        VocabularyAlbum saved = service.save(album);
        return ResponseEntity.ok(saved);
    }


    @PutMapping("/{id}")
    public ResponseEntity<VocabularyAlbum> update(@PathVariable Long id, @RequestBody VocabularyAlbum album) {
        album.setAlbumId(id);
        return ResponseEntity.ok(service.save(album));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/my-albums")
    public ResponseEntity<List<VocabularyAlbum>> getMyAlbums(HttpServletRequest request) {
        List<VocabularyAlbum> albums = service.findAllByUserId(request);
        return ResponseEntity.ok(albums);
    }
}
