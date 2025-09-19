package com.example.english_exam.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAnswerRequest {
    private String correctEnglish;
    private String correctVietnamese;
}
