package com.example.english_exam.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTestRequest {

    private String title;
    private String description;

    private Long examTypeId;

    // optional
    private Long classId;
    private Long chapterId;

    private Integer durationMinutes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private Integer maxAttempts;
}
