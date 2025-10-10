package com.example.english_exam.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateTestWithQuestionsRequest {
    // Thông tin cơ bản của Test
    private String title;
    private String description;
    private Long examTypeId;
    private Integer durationMinutes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private Integer maxAttempts;
    private List<PartWithQuestionsRequest> parts;
    private Long classId;
}