package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.user.AnswerResponse;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class AnswerAdminResponse {

    private final Long answerId;
    private final String answerText;
    private final String answerLabel;
    private final Boolean isCorrect;
}

