package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question.QuestionType;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAdminResponse {
    private Long questionId;
    private Long examTypeId; // 🟢 Thêm để FE biết kỳ thi nào
    private Long examPartId;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private PassageResponse passage; // 🟢 Thêm để FE hiển thị nội dung passage
    private Long testPartId;
    private List<AnswerAdminResponse> answers;
    private Long classId;
}
