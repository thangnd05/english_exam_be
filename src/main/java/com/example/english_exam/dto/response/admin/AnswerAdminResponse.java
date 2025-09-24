package com.example.english_exam.dto.response.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerAdminResponse {
    private Long answerId;
    private String answerText;
    private Boolean isCorrect;
    private String answerLabel;
}
