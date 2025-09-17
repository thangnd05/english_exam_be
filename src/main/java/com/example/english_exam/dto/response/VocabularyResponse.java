package com.example.english_exam.dto.response;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VocabularyResponse {
    private Long vocabId;
    private String word;
    private String phonetic;
    private String meaning;
    private String example;

    private Long albumId;
    private String albumName;
    private String albumDesc;   // 👉 thêm để biết mô tả album (optional)

    private String voiceUrl;   // 👈 thêm field này
    private LocalDateTime createdAt;
}


