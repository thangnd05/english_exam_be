package com.example.english_exam.dto.request;

import lombok.*;

import java.util.List;

/** Tạo nhiều câu hỏi thông thường vào kho (không passage). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateQuestionsToBankRequest {
    private Long examPartId;
    private Long classId;
    private Long chapterId;
    private List<NormalQuestionRequest> questions;
}
