package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ScoringConversionRequest {
    private Long examTypeId;
    private Long skillId;
    private Integer numCorrect;
    private Integer convertedScore;
}
