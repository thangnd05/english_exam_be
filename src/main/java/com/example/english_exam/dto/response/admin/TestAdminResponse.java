package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.user.TestResponse;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAdminResponse extends TestResponse {
    private Long createdBy;
    private LocalDateTime createdAt;
    private String bannerUrl;
    private Long classId;

}
