package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;



@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionRequest {
    private Long examPartId;
    private PassageRequest passage;
    private String questionText;
    private Question.QuestionType questionType;
    private List<AnswerRequest> answers;
    private Long testPartId;
}
