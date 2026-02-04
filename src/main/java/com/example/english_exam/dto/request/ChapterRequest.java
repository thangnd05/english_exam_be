package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ChapterRequest {
    private Long classId;
    private String title;
    private String description;
}
