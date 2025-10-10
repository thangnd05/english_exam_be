package com.example.english_exam.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {
    private String title;
    private String description;
    private Long examTypeId;
    private Integer durationMinutes;
    private String availableFrom; // gửi dạng "2025-10-08T10:00"
    private String availableTo;
    private Integer maxAttempts;
    private List<PartRequest> parts; // sử dụng DTO bạn đã có
    private Long classId;
}
