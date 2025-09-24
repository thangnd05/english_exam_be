package com.example.english_exam.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {
    private String title;
    private String description;
    private Long examTypeId;
    private Long studentId; // createdBy
    private Integer durationMinutes; // thêm field này, có thể null nếu dùng default
    private List<PartRequest> parts;

}
