package com.example.english_exam.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
public class ChapterResponse {

    private Long chapterId;
    private Long classId;
    private String title;
    private String description;
    private LocalDateTime createdAt;
}

