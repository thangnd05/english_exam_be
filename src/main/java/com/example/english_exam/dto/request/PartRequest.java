package com.example.english_exam.dto.request;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartRequest {
    @NotNull
    private Long examPartId; // Mã Part (ví dụ Part 1 Listening, Part 5 Reading)

    // Số câu cần lấy khi random
    @Min(1)
    private Integer numQuestions;

    // true = random theo numQuestions; false = chọn thủ công theo questionIds
    @Builder.Default
    private boolean random = true;

    // Dùng khi chọn thủ công
    private List<Long> questionIds;
}
