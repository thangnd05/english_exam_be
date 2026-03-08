package com.example.english_exam.dto.request;

import com.example.english_exam.models.UserVocabulary;
import lombok.Data;

@Data
public class UserVocabularyRequest {
    private Long vocabId;
    private UserVocabulary.Status status;
}
