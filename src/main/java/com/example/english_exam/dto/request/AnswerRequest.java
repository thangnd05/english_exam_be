package com.example.english_exam.dto.request;

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
