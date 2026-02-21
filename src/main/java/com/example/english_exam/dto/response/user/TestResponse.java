package com.example.english_exam.dto.response.user;

import com.example.english_exam.models.Test;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private Integer remainingAttempts;
    private Boolean canDoTest;
    private List<?> parts;

}

