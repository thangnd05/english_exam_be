package com.example.english_exam.dto.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PracticeCheckResponse {
    private Long questionId;
    private boolean correct;
    private String status; // learning / mastered
    private int correctCount;

    public PracticeCheckResponse(Long questionId, boolean correct, String status, int correctCount) {
        this.questionId = questionId;
        this.correct = correct;
        this.status = status;
        this.correctCount = correctCount;
    }
}

