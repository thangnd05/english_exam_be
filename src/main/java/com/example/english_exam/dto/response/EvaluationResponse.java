package com.example.english_exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EvaluationResponse {

    private Long id;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;
}
