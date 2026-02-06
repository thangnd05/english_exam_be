package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class TestPartRequest {
    private Long testId;
    private Long examPartId;
    private Integer numQuestions;
}