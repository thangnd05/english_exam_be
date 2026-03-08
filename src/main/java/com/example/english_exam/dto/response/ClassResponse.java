package com.example.english_exam.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassResponse {
    private Long classId;
    private String className;
    private String description;
    private Long teacherId;
    private LocalDateTime createdAt;
}
