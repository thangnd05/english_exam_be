package com.example.english_exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassStudentResponse {
    private Long classId;
    private String className;
    private String teacherName;
}
