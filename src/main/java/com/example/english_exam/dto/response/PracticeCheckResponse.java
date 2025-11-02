package com.example.english_exam.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PracticeCheckResponse {
    private Long vocabId;
    private boolean correct;
    private String status; // learning / mastered
    private int correctCount;
}
