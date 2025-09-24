package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question.QuestionType;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAdminResponse {
    private Long questionId;
    private Long examPartId;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private Long testPartId;

    private PassageResponse passage;
    private List<AnswerAdminResponse> answers;    // có isCorrect
}
