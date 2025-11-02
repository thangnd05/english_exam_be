package com.example.english_exam.dto.response;

import com.example.english_exam.models.PracticeQuestion;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
public class PracticeQuestionResponse {
    private Long id;
    private Long vocabId;
    private PracticeQuestion.QuestionType type;
    private String questionText;
    private String audioUrl; // chỉ dùng cho LISTENING
    private List<PracticeOptionResponse> options;
    private PracticeAnswerResponse answer;

    public PracticeQuestionResponse(Long id, Long vocabId, PracticeQuestion.QuestionType type, String questionText, String audioUrl, List<PracticeOptionResponse> options, PracticeAnswerResponse answer) {
        this.id = id;
        this.vocabId = vocabId;
        this.type = type;
        this.questionText = questionText;
        this.audioUrl = audioUrl;
        this.options = options;
        this.answer = answer;
    }
}
