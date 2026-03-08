package com.example.english_exam.dto.response;

import com.example.english_exam.models.UserVocabulary;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVocabularyResponse {
    private Long id;
    private Long userId;
    private Long vocabId;
    private UserVocabulary.Status status;
    private LocalDateTime lastReviewed;
    private Integer correctCount;
}
