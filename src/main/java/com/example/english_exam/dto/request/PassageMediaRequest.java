package com.example.english_exam.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassageMediaRequest {

    private Long passageId;

    private String mediaUrl;

    private String mediaType; // IMAGE / AUDIO
}
