package com.example.english_exam.dto.response.admin;

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
public class QuestionGroupAdminResponse {

    private PassageResponse passage; // có thể null

    private List<QuestionAdminResponse> questions;
}
