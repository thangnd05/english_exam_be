package com.example.english_exam.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateQuestionsWithPassageRequest {
    private Long examPartId;
    private PassageRequest passage; // chứa content, passageType, mediaUrl (nếu có)
    private List<NormalQuestionRequest> questions; // danh sách câu hỏi cùng passage
    private Long classId;
}
