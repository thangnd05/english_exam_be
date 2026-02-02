package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class EvaluationRequest {
    private String content;
    private Integer rating;
}
