package com.example.english_exam.dto.response.user;

import com.example.english_exam.models.Test;
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

    public TestResponse(Test test) {
        // Sao chép dữ liệu từ đối tượng 'test' được truyền vào
        this.testId = test.getTestId();
        this.title = test.getTitle();
        this.description = test.getDescription();
        this.examTypeId = test.getExamTypeId();
        this.createdBy = test.getCreatedBy();
        this.createdAt = test.getCreatedAt();
        this.bannerUrl = test.getBannerUrl();
        this.durationMinutes = test.getDurationMinutes();
        this.availableFrom = test.getAvailableFrom();
        this.availableTo = test.getAvailableTo();
        this.status = test.calculateStatus().name();
        this.maxAttempts = test.getMaxAttempts();

        // Đối với người dùng chưa đăng nhập, các trường này sẽ là null
        this.attemptsUsed = null;
        this.remainingAttempts = null;
        this.parts = null; // Hoặc bạn có thể map sang parts công khai nếu cần
    }
}
