package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class VocabularyUpdateRequest {
    private String word;
    private String meaning;
    private String example;
    private Long albumId;
}
