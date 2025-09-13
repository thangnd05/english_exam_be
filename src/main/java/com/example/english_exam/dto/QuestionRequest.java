package com.example.english_exam.dto;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    private Long examPartId;
    private Long passageId; // nullable
    private String questionText;
    private Question.QuestionType questionType;
    private List<AnswerRequest> answers;
}
