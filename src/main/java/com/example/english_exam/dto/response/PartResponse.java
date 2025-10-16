package com.example.english_exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartResponse {
    private Long examPartId;     // ID của phần thi (Reading, Listening...)
    private Integer numQuestions; // Số lượng câu hỏi trong phần
    private List<Long> questionIds; // Danh sách ID các câu hỏi thuộc phần
}
