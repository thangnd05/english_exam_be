package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ExamPartRequest {
    private Long examTypeId;
    private String name;
    private String description;
    private Integer defaultNumQuestions;
    private Long skillId;
}
