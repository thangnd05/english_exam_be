package com.example.english_exam.dto.response;

import lombok.Data;

@Data
public class ScoringConversionResponse {
    private Long conversionId;
    private Long examTypeId;
    private Long skillId;
    private Integer numCorrect;
    private Integer convertedScore;
}
