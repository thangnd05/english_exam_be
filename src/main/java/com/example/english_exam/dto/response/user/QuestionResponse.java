package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question.QuestionType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private Long examPartId;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private Long testPartId;

    private PassageResponse passage;          // thay vì passageId
    private List<AnswerResponse> answers;   // chỉ có answerId, answerText, answerLabel
}
