package com.example.english_exam.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeCheckRequest {
    private Long userId;           // Người dùng nhập tạm thời
    private Long questionId;
    private String userEnglish;   // người dùng nhập từ tiếng Anh
    private String userVietnamese; // người dùng nhập nghĩa tiếng Việt
    private Long selectedOptionId;

}
