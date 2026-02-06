package com.example.english_exam.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTestRequest {
    private String title;
    private String description;
    private Long examTypeId;
    private Integer durationMinutes;
    private String bannerUrl;
    private Integer maxAttempts;
    private Long classId;
    private Long chapterId;

    // Hỗ trợ cấu hình thời gian mở/đóng đề
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
}