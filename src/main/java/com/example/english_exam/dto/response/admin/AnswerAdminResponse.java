package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.user.AnswerResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerAdminResponse extends AnswerResponse {
    private Boolean isCorrect;

}
