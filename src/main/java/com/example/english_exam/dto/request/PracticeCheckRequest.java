package com.example.english_exam.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PracticeCheckRequest {
    private Long vocabId;
    private String type; // MULTICHOICE / LISTENING_EN
    private String selectedOptionText; // nếu MULTICHOICE
    private String userEnglish; // nếu LISTENING_EN
    private String userVietnamese; // nếu LISTENING_EN
}
