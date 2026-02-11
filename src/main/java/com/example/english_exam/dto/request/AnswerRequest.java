package com.example.english_exam.dto.request;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {
    private String answerText;
    private Boolean isCorrect;
    private String answerLabel; // ví dụ: A, B, C, D
}
