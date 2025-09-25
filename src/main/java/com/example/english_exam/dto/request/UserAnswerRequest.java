package com.example.english_exam.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerRequest {
    private Long userTestId;       // thêm luôn userTestId
    private Long questionId;
    private Long selectedAnswerId; // null nếu tự luận
    private String answerText;     // cho tự luận
}
