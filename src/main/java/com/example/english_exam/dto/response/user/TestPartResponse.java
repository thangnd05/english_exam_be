package com.example.english_exam.dto.response.user;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TestPartResponse {
    private Long testPartId;
    private Long examPartId;
    private List<QuestionGroupResponse> questionGroups; // Chuyển từ List<QuestionResponse> sang Group

    public TestPartResponse(Long testPartId, Long examPartId, List<QuestionGroupResponse> questionGroups) {
        this.testPartId = testPartId;
        this.examPartId = examPartId;
        this.questionGroups = questionGroups;
    }
}
