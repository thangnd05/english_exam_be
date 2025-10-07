package com.example.english_exam.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestRequest {
    private String title;
    private String description;
    private Long examTypeId;
    private Long createBy;
    private Integer durationMinutes;
    private String availableFrom; // gửi dạng "2025-10-08T10:00"
    private String availableTo;
    private Integer maxAttempts;
    private List<PartRequest> parts; // sử dụng DTO bạn đã có
}
