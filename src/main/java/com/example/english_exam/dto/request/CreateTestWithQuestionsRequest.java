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
    private Long createBy;
    private Integer durationMinutes;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private Integer maxAttempts;

    // Danh sách các phần của Test, mỗi phần chứa các câu hỏi cần tạo
    private List<PartWithQuestionsRequest> parts;
}