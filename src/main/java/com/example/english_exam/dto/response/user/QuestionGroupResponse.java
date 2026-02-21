package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGroupResponse {
    private PassageResponse passage; // Đoạn văn chung cho nhóm (có thể null nếu là câu đơn)
    private List<QuestionResponse> questions; // Danh sách câu hỏi trong nhóm
}
