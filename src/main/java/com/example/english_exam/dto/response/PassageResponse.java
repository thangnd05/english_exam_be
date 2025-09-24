package com.example.english_exam.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassageResponse {
    private Long passageId;
    private String content;
    private String mediaUrl;
    private String passageType;
}
