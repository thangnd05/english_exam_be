package com.example.english_exam.dto.response.user;

import com.example.english_exam.models.Test;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {
    protected Long testId;
    protected String title;
    protected String description;
    protected Long examTypeId;
    protected Long createdBy;
    protected LocalDateTime createdAt;
    protected String bannerUrl;
    protected Integer durationMinutes;
    protected LocalDateTime availableFrom;
    protected LocalDateTime availableTo;
    protected String status;
    protected Integer maxAttempts;
    protected Integer attemptsUsed;
    protected Integer remainingAttempts;
    protected Boolean canDoTest;
    /** User: List<TestPartResponse>; Admin (TestAdminResponse): List<TestPartAdminResponse>. */
    protected List<?> parts;

    /** Tạo TestResponse rút gọn từ entity Test (dùng khi list test, chưa load parts). */
    public TestResponse(Test test) {
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
        this.attemptsUsed = null;
        this.remainingAttempts = null;
        this.canDoTest = true;
        this.parts = null;
    }
}

