package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPartResponse {
    private Long testPartId;
    private Long examPartId;
    private int numQuestions;
    private PassageResponse passage; // ✅ Chuyển passage ra đây

    private List<QuestionResponse> questions;
}
