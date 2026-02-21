package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long questionId;
    private Long examPartId;
    private String questionText;
    private Question.QuestionType questionType;
    private Long testPartId;
    private List<AnswerResponse> answers;


}

