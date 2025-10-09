package com.example.english_exam.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testId;

    @Column(nullable = false, length = 255)
    private String title;

    private String description;

    @Column(nullable = false)
    private Long examTypeId;

    private Long createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Thời gian giới hạn làm bài (null = không giới hạn)
    @Column(nullable = true)
    private Integer durationMinutes;

    // Khoảng thời gian mở đề (optional)
    private LocalDateTime availableFrom;

    private LocalDateTime availableTo;


    @Column(length = 500) // đủ dài để chứa URL
    private String bannerUrl;


    // Số lần làm bài cho phép (null = không giới hạn)
    private Integer maxAttempts;

    @Column(name = "class_id")
    private Long classId; // FK -> exam_parts



    // ✅ Method tính trạng thái thực tế
    public TestStatus calculateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (availableFrom == null && availableTo == null) {
            return TestStatus.OPEN; // luôn mở
        }
        if (availableFrom != null && now.isBefore(availableFrom)) {
            return TestStatus.NOT_STARTED;
        }
        if (availableTo != null && now.isAfter(availableTo)) {
            return TestStatus.ENDED;
        }
        return TestStatus.OPEN;
    }


}
