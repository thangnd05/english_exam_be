// response/TestResponse.java
package com.example.english_exam.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {
    private Long testId;
    private String title;
    private String description;
    private Long examTypeId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<TestPartResponse> parts;
}
