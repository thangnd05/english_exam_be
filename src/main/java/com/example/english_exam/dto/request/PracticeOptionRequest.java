package com.example.english_exam.dto.request;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeOptionRequest {
    private String optionText;
    private boolean isCorrect;
}

