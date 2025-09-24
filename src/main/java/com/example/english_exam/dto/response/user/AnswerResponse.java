package com.example.english_exam.dto.response.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long answerId;
    private String answerText;
    private String answerLabel; // đổi tên từ label
}
