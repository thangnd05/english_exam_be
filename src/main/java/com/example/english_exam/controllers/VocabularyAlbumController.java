package com.example.english_exam.controllers;

import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.services.VocabularyAlbumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
public class VocabularyAlbumController {
    private final VocabularyAlbumService service;

    public VocabularyAlbumController(VocabularyAlbumService service) {
        this.service = service;
    }

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
    public ResponseEntity<VocabularyAlbum> create(@RequestBody VocabularyAlbum album) {
        return ResponseEntity.ok(service.save(album));
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
}
