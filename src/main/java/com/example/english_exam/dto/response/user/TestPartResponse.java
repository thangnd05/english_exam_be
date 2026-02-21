package com.example.english_exam.dto.response.user;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TestPartResponse {
    private Long testPartId;
    private Long examPartId;
    private Integer numQuestions;
    private List<QuestionGroupResponse> questionGroups; // Chuyển từ List<QuestionResponse> sang Group

    public TestPartResponse(Long testPartId, Long examPartId, Integer numQuestions, List<QuestionGroupResponse> questionGroups) {
        this.testPartId = testPartId;
        this.examPartId = examPartId;
        this.numQuestions = numQuestions;
        this.questionGroups = questionGroups;
    }
}
