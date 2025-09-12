package com.example.test.controller;


import com.example.test.models.Images;
import com.example.test.services.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<Images> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam("postId") Long postId) throws IOException {
        Images savedImage = imageService.uploadImage(file, postId);
        return ResponseEntity.ok(savedImage);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Optional<Images>> getImagesByPostId(@PathVariable Long postId) {
        Optional<Images> images = imageService.getImagesByPostId(postId);
        if (images.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(images);
    }

    @GetMapping
    public ResponseEntity<List<Images>> getAllImages() {
        List<Images> images = imageService.getAllImages();
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Images> getImageById(@PathVariable Long id) {
        Optional<Images> image = imageService.getImageById(id);
        return image.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Images> updateImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Images updatedImage = imageService.updateImage(id, file);
        return ResponseEntity.ok(updatedImage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) throws IOException {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}



