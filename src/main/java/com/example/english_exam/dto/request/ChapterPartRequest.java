package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ChapterPartRequest {

    private Long examPartId;
    private Integer numQuestions;
}
