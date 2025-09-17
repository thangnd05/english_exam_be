package com.example.english_exam.dto.request;


import lombok.Data;

@Data
public class VocabularyRequest {
    private String word;
//    private String phonetic;
    private String meaning;
    private String example;
    private Long albumId;          // nếu chọn album có sẵn
    private String newAlbumName;   // nếu muốn tạo album mới
    private String newAlbumDesc;   // mô tả album mới
    private Long userId;
}
