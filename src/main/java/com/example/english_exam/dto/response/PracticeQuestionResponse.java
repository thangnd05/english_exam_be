package com.example.english_exam.dto.response;


import com.example.english_exam.models.PracticeQuestion;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionResponse {
    private Long id;
    private Long vocabId;
    private PracticeQuestion.QuestionType type;
    private String questionText;
    private String audioUrl;
    private List<PracticeOptionResponse> options;
    private PracticeAnswerResponse answer;
}

