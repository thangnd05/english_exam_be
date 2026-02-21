package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestPartAdminResponse {

    private Long testPartId;

    private Long examPartId;

    private Integer numQuestions;

    private List<QuestionGroupAdminResponse> questionGroups;
}
