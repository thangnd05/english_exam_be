package com.example.english_exam.dto.response;

import com.example.english_exam.models.PracticeQuestion;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionResponse {
    private Long id;
    private Long vocabId;
    private PracticeQuestion.QuestionType type;
    private String questionText;
    private String audioUrl; // chỉ dùng cho LISTENING
    private List<PracticeOptionResponse> options;
    private PracticeAnswerResponse answer;

    public PracticeQuestionResponse(Long id, Long vocabId, PracticeQuestion.QuestionType type, String questionText, List<PracticeOptionResponse> optionResponses, PracticeAnswerResponse answerResponse) {
    }
}
