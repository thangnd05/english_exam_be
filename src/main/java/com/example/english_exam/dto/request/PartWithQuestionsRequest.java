package com.example.english_exam.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PartWithQuestionsRequest {
    private Long examPartId;
    private List<NormalQuestionRequest> questions;
}