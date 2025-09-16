// request/PartRequest.java
package com.example.english_exam.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartRequest {
    private Long examPartId;
    private int numQuestions;
}
