package com.example.english_exam.models;

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

    @Column(nullable = true)
    private Integer durationMinutes; // thời gian riêng cho part (nếu muốn)

    @Column(length = 500) // đủ dài để chứa URL
    private String bannerUrl;
}

