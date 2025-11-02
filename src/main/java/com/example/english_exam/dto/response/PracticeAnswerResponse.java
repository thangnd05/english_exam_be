package com.example.english_exam.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAnswerResponse {
    private Long id;
    private String correctEnglish;
    private String correctVietnamese;

    public PracticeAnswerResponse(String correctEnglish, String correctVietnamese) {
        this.correctEnglish = correctEnglish;
        this.correctVietnamese = correctVietnamese;
    }
}

