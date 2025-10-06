package com.example.english_exam.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTestResponse {
    private Long userTestId;
    private Long userId;     // 🟢 thêm
    private Long testId;     // 🟢 thêm
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer totalScore;
    private String status;
}
