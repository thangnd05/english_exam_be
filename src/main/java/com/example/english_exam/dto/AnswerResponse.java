package com.example.english_exam.dto;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long answerId;
    private String answerText;
    private Boolean isCorrect;
    private String answerLabel; // đổi tên từ label
}

