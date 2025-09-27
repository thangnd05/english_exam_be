package com.example.english_exam.dto.response.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {
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

    private String status;
    private Integer maxAttempts;
    private Integer attemptsUsed;
    private Integer remainingAttempts; // thêm




    private List<TestPartResponse> parts;
}
