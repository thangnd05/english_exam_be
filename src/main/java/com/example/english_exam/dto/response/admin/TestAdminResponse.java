package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.user.TestResponse;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAdminResponse extends TestResponse {
    private Long classId;

    /** Constructor đầy đủ cho admin (parts là List<TestPartAdminResponse>). */
    public TestAdminResponse(Long testId, String title, String description, Long examTypeId,
                             Long createdBy, LocalDateTime createdAt, String bannerUrl,
                             Integer durationMinutes, LocalDateTime availableFrom, LocalDateTime availableTo,
                             String status, Integer maxAttempts, List<TestPartAdminResponse> parts, Long classId) {
        super(testId, title, description, examTypeId, createdBy, createdAt, bannerUrl,
                durationMinutes, availableFrom, availableTo, status, maxAttempts,
                null, null, null, parts);
        this.classId = classId;
    }
}
