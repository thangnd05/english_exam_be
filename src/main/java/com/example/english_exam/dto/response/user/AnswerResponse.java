package com.example.english_exam.dto.response.user;

import lombok.*;

@Getter
@Setter // 🚀 Thêm Setter để BE có thể gán lại nhãn A, B, C, D
@Builder
@AllArgsConstructor
@NoArgsConstructor // Thêm để hỗ trợ các thư viện mapping nếu cần
public class AnswerResponse {
    private Long answerId;
    private String answerText;
    private String answerLabel; // Bỏ final
}