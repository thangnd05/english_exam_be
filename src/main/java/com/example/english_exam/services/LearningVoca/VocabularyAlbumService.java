package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.dto.request.VocabularyAlbumRequest;
import com.example.english_exam.dto.response.VocabularyAlbumResponse;
import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.repositories.VocabularyAlbumRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class VocabularyAlbumService {

    private final VocabularyAlbumRepository repository;
    private final AuthUtils authUtils;

    // =========================
    // GET ALL
    // =========================
    public List<VocabularyAlbumResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET BY ID
    // =========================
    public VocabularyAlbumResponse findById(Long id) {
        VocabularyAlbum album = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album không tồn tại"));

        return toResponse(album);
    }

    // =========================
    // CREATE
    // =========================
    public VocabularyAlbumResponse create(
            VocabularyAlbumRequest request,
            HttpServletRequest httpRequest
    ) {

        Long userId = authUtils.getUserId(httpRequest);

        VocabularyAlbum album = new VocabularyAlbum();
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setUserId(userId);

        album = repository.save(album);

        return toResponse(album);
    }

    // =========================
    // UPDATE
    // =========================
    public VocabularyAlbumResponse update(Long id, VocabularyAlbumRequest request) {

        VocabularyAlbum album = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album không tồn tại"));

        album.setName(request.getName());
        album.setDescription(request.getDescription());

        album = repository.save(album);

        return toResponse(album);
    }

    // =========================
    // DELETE
    // =========================
    public void delete(Long id) {

        VocabularyAlbum album = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album không tồn tại"));

        repository.delete(album);
    }

    // =========================
    // GET MY ALBUMS
    // =========================
    public List<VocabularyAlbumResponse> findAllByUserId(HttpServletRequest request) {

        Long currentUserId = authUtils.getUserId(request);

        return repository.findAllByUserId(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // MAPPER
    // =========================
    private VocabularyAlbumResponse toResponse(VocabularyAlbum album) {

        VocabularyAlbumResponse response = new VocabularyAlbumResponse();

        response.setAlbumId(album.getAlbumId());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setUserId(album.getUserId());
        response.setCreatedAt(album.getCreatedAt());

        return response;
    }
}