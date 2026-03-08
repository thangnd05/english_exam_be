package com.example.english_exam.dto.response;

import com.example.english_exam.models.ClassMember;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassMemberResponse {
    private Long id;
    private Long classId;
    private Long userId;
    private ClassMember.MemberStatus status;
    private LocalDateTime joinedAt;
}
