package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;



@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QuestionCreateRequest {
    private Long examPartId;
    private Long classId;
    private Long chapterId;
    private PassageRequest passage; // optional
    private String questionText;
    private Question.QuestionType questionType;
    private List<AnswerRequest> answers;
}

