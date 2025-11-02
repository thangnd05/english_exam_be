package com.example.english_exam.dto.response;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PracticeQuestionResponse {
    private Long vocabId;
    private String type; // MULTICHOICE hoặc LISTENING_EN
    private String questionText;
    private String voiceUrl;
    private String word;
    private String meaning;
    private List<String> options; // 4 đáp án nghĩa
}
