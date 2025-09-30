package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question.QuestionType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NormalQuestionRequest {
    private PassageRequest passage; // ✅ THÊM DÒNG NÀY
    private String questionText;
    private QuestionType questionType;
    private List<AnswerRequest> answers;
}
