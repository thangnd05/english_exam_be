package com.example.english_exam.dto.response;

import lombok.Data;

@Data
public class ExamPartResponse {
    private Long examPartId;
    private Long examTypeId;
    private String name;
    private String description;
    private Integer defaultNumQuestions;
    private Long skillId;
}
