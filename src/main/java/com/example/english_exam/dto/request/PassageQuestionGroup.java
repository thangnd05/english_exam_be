package com.example.english_exam.dto.request;

import lombok.Data;

import java.util.List;
@Data
public class PassageQuestionGroup {

    private PassageRequest passage;
    private List<NormalQuestionRequest> questions;
}

