package com.example.english_exam.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassageMediaResponse {

    private Long id;

    private Long passageId;

    private String mediaUrl;

    private String mediaType;
}
