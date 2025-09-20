package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;



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

    public Long getExamPartId() {
        return examPartId;
    }

    public void setExamPartId(Long examPartId) {
        this.examPartId = examPartId;
    }

    public Long getPassageId() {
        return passageId;
    }

    public void setPassageId(Long passageId) {
        this.passageId = passageId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Question.QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(Question.QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<AnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerRequest> answers) {
        this.answers = answers;
    }

    public Long getTestPartId() {
        return testPartId;
    }

    public void setTestPartId(Long testPartId) {
        this.testPartId = testPartId;
    }
}
