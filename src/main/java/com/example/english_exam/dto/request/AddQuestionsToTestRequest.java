package com.example.english_exam.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AddQuestionsToTestRequest {

    private Long testPartId;
    private List<Long> questionIds;
}
