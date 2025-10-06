package com.example.english_exam.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnswerResponse {
    private Long questionId;
    private Long selectedAnswerId;
    private String answerText;
}
