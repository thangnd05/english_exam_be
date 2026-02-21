package com.example.english_exam.dto.response.user;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class AnswerResponse {
    private final Long answerId;
    private final String answerText;
    private final String answerLabel;
}

