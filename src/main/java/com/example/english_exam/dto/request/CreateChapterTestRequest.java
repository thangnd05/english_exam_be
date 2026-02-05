package com.example.english_exam.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateChapterTestRequest {

    private Long classId;      // ✅ bắt buộc
    private Long chapterId;    // ✅ bắt buộc

    private String title;
    private String description;
    private Long examTypeId;

    private Integer durationMinutes;
    private String availableFrom;
    private String availableTo;

    private Integer maxAttempts;

    // ✅ mỗi part random bao nhiêu câu
    private List<ChapterPartRequest> parts;
}
