// response/TestPartResponse.java
package com.example.english_exam.dto.response.user;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPartResponse {
    private Long testPartId;
    private Long examPartId;
    private int numQuestions;
    private List<QuestionResponse> questions;
}
