package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ExamTypeRequest {
    private String name;
    private String description;
    private Integer durationMinutes;
    private String scoringMethod;
}
