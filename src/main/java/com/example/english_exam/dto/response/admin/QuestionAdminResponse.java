package com.example.english_exam.dto.response.admin;

import com.example.english_exam.models.Question;
import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuestionAdminResponse {

    private final Long questionId;
    private final Long examPartId;
    private final String questionText;
    private final Question.QuestionType questionType;
    private final String explanation;

    // Admin-specific fields
    private final Long examTypeId;
    private final Long classId;
    private final Boolean isBank;

    private final List<AnswerAdminResponse> answers;
}