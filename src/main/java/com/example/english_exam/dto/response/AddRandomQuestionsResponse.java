package com.example.english_exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddRandomQuestionsResponse {

    /** Số câu đã thêm vào test part. */
    private int addedCount;
}
