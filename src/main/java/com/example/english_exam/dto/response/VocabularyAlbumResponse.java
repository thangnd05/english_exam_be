package com.example.english_exam.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VocabularyAlbumResponse {
    private Long albumId;
    private String name;
    private String description;
    private Long userId;
    private LocalDateTime createdAt;
}
