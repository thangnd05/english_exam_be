package com.example.english_exam.dto.request;

import com.example.english_exam.models.Passage; // Giả sử bạn có enum PassageType
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PassageRequest {
    private String content;
    private String mediaUrl; // Optional
    private Passage.PassageType passageType;
}