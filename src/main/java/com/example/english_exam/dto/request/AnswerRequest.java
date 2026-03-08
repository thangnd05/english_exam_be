package com.example.english_exam.dto.request;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {
    /** Có giá trị khi sửa: cập nhật đáp án đó; null = thêm mới. */
    private Long answerId;
    private String answerText;
    private Boolean isCorrect;
    private String answerLabel; // ví dụ: A, B, C, D
}
