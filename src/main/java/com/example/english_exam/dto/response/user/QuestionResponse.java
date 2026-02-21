package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private Long examPartId;
    private String questionText;
    private Question.QuestionType questionType;
    private String explanation;

    private Long testPartId;
    private List<AnswerResponse> answers;

    public QuestionResponse(Long questionId, Long examPartId, String questionText, Question.QuestionType questionType, String explanation, Long testPartId, List<AnswerResponse> answers) {
        this.questionId = questionId;
        this.examPartId = examPartId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.explanation = explanation;
        this.testPartId = testPartId;
        this.answers = answers;
    }
}

