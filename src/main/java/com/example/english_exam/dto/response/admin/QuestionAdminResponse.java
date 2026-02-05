package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAdminResponse extends QuestionResponse {
    private Long examTypeId;
    private PassageResponse passage;
    private Long classId;
}
