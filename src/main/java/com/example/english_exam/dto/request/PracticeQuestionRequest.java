package com.example.english_exam.dto.request;

import com.example.english_exam.models.PracticeQuestion;
import lombok.*;

import java.util.List;



public class PracticeQuestionRequest {
    private Long vocabId;
    private PracticeQuestion.QuestionType type;
    private String questionText;
    private List<PracticeOptionRequest> options; // cho MULTICHOICE
    private PracticeAnswerRequest answer;

    public PracticeQuestionRequest(Long vocabId, PracticeQuestion.QuestionType type, String questionText, List<PracticeOptionRequest> options, PracticeAnswerRequest answer) {
        this.vocabId = vocabId;
        this.type = type;
        this.questionText = questionText;
        this.options = options;
        this.answer = answer;
    }

    public Long getVocabId() {
        return vocabId;
    }

    public void setVocabId(Long vocabId) {
        this.vocabId = vocabId;
    }

    public PracticeQuestion.QuestionType getType() {
        return type;
    }

    public void setType(PracticeQuestion.QuestionType type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<PracticeOptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<PracticeOptionRequest> options) {
        this.options = options;
    }

    public PracticeAnswerRequest getAnswer() {
        return answer;
    }

    public void setAnswer(PracticeAnswerRequest answer) {
        this.answer = answer;
    }
    // cho LISTENING/WRITING
}

