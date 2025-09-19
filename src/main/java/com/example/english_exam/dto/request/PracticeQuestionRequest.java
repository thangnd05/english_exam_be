package com.example.english_exam.dto.request;


import com.example.english_exam.models.PracticeQuestion;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionRequest {
    private Long vocabId;
    private PracticeQuestion.QuestionType type;
    private String questionText;
    private String audioUrl; // optional

    private List<PracticeOptionRequest> options; // cho MULTICHOICE
    private PracticeAnswerRequest answer;        // cho LISTENING/WRITING
}
