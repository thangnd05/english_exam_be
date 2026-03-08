package com.example.english_exam.dto.request;

import lombok.Data;

@Data
public class ClassMemberActionRequest {
    private Long classId;
    private Long userId;
}
