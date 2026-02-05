package com.example.english_exam.dto.request;

import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;

/**
 * Tạo câu hỏi "tức thì" và gắn thẳng vào một part của đề (không lưu kho, isBank = false).
 * Dùng khi giáo viên trên lớp đặt câu hỏi rồi đưa vào đề.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateQuestionAndAttachRequest {
    private Long testPartId;
    private Long classId;
    private Long chapterId;
    private PassageRequest passage;
    private String questionText;
    private Question.QuestionType questionType;
    private List<AnswerRequest> answers;
}
