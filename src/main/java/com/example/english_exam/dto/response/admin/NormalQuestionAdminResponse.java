package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question.QuestionType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NormalQuestionAdminResponse {
    private Long questionId;
    private Long examPartId;   // default exam part
    private String questionText;
    private QuestionType questionType;

    private PassageResponse passage;
    private List<AnswerAdminResponse> answers;  // có isCorrect
}
