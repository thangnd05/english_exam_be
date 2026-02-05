package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.user.AnswerResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class AnswerAdminResponse extends AnswerResponse {
    private Boolean isCorrect;

    public AnswerAdminResponse(Long answerId, String answerText, Boolean isCorrect, String answerLabel) {
        super(answerId, answerText, answerLabel);
        this.isCorrect = isCorrect;
    }
}
