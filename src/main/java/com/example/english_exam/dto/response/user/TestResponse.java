package com.example.english_exam.dto.response.user;

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
    protected Integer durationMinutes;
    protected LocalDateTime availableFrom;
    protected LocalDateTime availableTo;
    protected String status;
    protected Integer maxAttempts;
    protected Integer attemptsUsed;
    protected Integer remainingAttempts;
    protected Boolean canDoTest;
    protected List<TestPartResponse> parts;
}

