package com.example.english_exam.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class BulkQuestionWithPassageRequest {

    private Long examPartId;

    // 🔑 bắt buộc
    private Long classId;
    private Long chapterId;

    private PassageRequest passage; // chung cho tất cả question
    private List<NormalQuestionRequest> questions;
}
