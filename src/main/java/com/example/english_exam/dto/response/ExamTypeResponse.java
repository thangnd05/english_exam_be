package com.example.english_exam.dto.response;

import lombok.Data;

@Data
public class ExamTypeResponse {
    private Long examTypeId;
    private String name;
    private String description;
    private Integer durationMinutes;
    private String scoringMethod;
}
