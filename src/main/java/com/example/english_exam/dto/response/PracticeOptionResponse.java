package com.example.english_exam.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeOptionResponse {
    private Long id;
    private String optionText;
    private boolean isCorrect;

    public PracticeOptionResponse(Long id, String optionText) {
        this.id = id;
        this.optionText = optionText;
    }
}

