package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.PassageMediaRequest;
import com.example.english_exam.dto.response.PassageMediaResponse;
import com.example.english_exam.services.PassageMediaService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passage-media")
@AllArgsConstructor
public class PassageMediaController {

    private final PassageMediaService service;

    // CREATE
    @PostMapping
    public ResponseEntity<PassageMediaResponse> create(
            @RequestBody PassageMediaRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<PassageMediaResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // GET BY PASSAGE
    @GetMapping("/by-passage/{passageId}")
    public ResponseEntity<List<PassageMediaResponse>> getByPassage(
            @PathVariable Long passageId) {
        return ResponseEntity.ok(service.getByPassageId(passageId));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<PassageMediaResponse> update(
            @PathVariable Long id,
            @RequestBody PassageMediaRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
