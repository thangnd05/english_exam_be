package com.example.english_exam.dto.request;


import lombok.Data;

@Data
public class VocabularyRequest {

    private String word;
    private String phonetic;
    private String meaning;
    private String example;

    private Long albumId;        // chọn album có sẵn

    private String newAlbumName; // nếu tạo album mới
    private String newAlbumDesc;

    private Long userId;

    private String voiceUrl;     // audio pronunciation
}