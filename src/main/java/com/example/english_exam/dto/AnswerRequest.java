package com.example.english_exam.dto;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {
    private String answerText;
    private Boolean isCorrect;
    private String label; // ví dụ: A, B, C, D
}
