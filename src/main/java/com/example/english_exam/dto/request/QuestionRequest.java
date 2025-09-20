package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;



@Setter
@Getter
public class QuestionRequest {
    private Long examPartId;
    private Long passageId; // nullable
    private String questionText;
    private Question.QuestionType questionType;
    private List<AnswerRequest> answers;
    private Long testPartId;

    public QuestionRequest(Long examPartId, Long passageId, String questionText, Question.QuestionType questionType, List<AnswerRequest> answers, Long testPartId) {
        this.examPartId = examPartId;
        this.passageId = passageId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.answers = answers;
        this.testPartId = testPartId;
    }

}
