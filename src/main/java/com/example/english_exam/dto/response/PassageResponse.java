package com.example.english_exam.dto.response;

import com.example.english_exam.models.Passage;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassageResponse {
    private Long passageId;
    private String content;
    private String mediaUrl;
    private Passage.PassageType passageType;
}
