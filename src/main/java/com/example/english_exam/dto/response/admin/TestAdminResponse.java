package com.example.english_exam.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAdminResponse {
    private Long testId;
    private String title;
    private String description;
    private Long examTypeId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private String bannerUrl;
    private Integer durationMinutes;

    private LocalDateTime availableFrom;

    private LocalDateTime availableTo;

    private String status; // ✅ thêm status trả về FE
    private Integer maxAttempts;



    private List<TestPartAdminResponse> parts; // chứa QuestionAdminResponse
}
