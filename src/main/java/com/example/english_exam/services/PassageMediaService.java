package com.example.english_exam.services;

import com.example.english_exam.dto.request.PassageMediaRequest;
import com.example.english_exam.dto.response.PassageMediaResponse;
import com.example.english_exam.models.PassageMedia;
import com.example.english_exam.repositories.PassageMediaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PassageMediaService {

    private final PassageMediaRepository repository;

    public PassageMediaService(PassageMediaRepository repository) {
        this.repository = repository;
    }

    // 🔹 CREATE
    public PassageMediaResponse create(PassageMediaRequest request) {

        PassageMedia media = new PassageMedia();
        media.setPassageId(request.getPassageId());
        media.setMediaUrl(request.getMediaUrl());
        media.setMediaType(PassageMedia.MediaType.valueOf(request.getMediaType()));

        media = repository.save(media);

        return toResponse(media);
    }

    // 🔹 GET BY ID
    public PassageMediaResponse getById(Long id) {

        PassageMedia media = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại"));

        return toResponse(media);
    }

    // 🔹 GET BY PASSAGE
    public List<PassageMediaResponse> getByPassageId(Long passageId) {

        return repository.findByPassageId(passageId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 🔹 UPDATE
    public PassageMediaResponse update(Long id, PassageMediaRequest request) {

        PassageMedia media = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media không tồn tại"));

        media.setMediaUrl(request.getMediaUrl());
        media.setMediaType(PassageMedia.MediaType.valueOf(request.getMediaType()));

        return toResponse(repository.save(media));
    }

    // 🔹 DELETE
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // 🔹 MAPPER
    private PassageMediaResponse toResponse(PassageMedia media) {
        return new PassageMediaResponse(
                media.getId(),
                media.getPassageId(),
                media.getMediaUrl(),
                media.getMediaType().name()
        );
    }
}
