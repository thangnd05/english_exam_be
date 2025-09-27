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
    private Long createBy;          // createdBy
    private Integer durationMinutes; // null = không giới hạn

    // Thời gian mở bài (optional)
    private LocalDateTime availableFrom;

    private LocalDateTime availableTo;

    private Integer maxAttempts;

    private List<PartRequest> parts;
}
