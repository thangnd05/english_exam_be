package com.example.english_exam.dto;

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
    private Long passageId;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private List<AnswerResponse> answers;
}
