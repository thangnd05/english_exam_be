package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question.QuestionType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NormalQuestionUserResponse {
    private Long questionId;
    private Long examPartId;   // default exam part
    private String questionText;
    private QuestionType questionType;

    private PassageResponse passage;
    private List<AnswerResponse> answers;   // không có isCorrect
}
