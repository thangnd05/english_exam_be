package com.example.english_exam.dto.response.admin;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPartAdminResponse {
    private Long testPartId;
    private Long examPartId;
    private int numQuestions;
    private List<QuestionAdminResponse> questions;  // dùng admin version
}
