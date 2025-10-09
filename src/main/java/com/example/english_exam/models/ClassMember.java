package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId; // FK -> classes.class_id

    @Column(name = "user_id", nullable = false)
    private Long userId; // FK -> users.user_id

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private MemberStatus status = MemberStatus.PENDING;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    // enum trạng thái
    public enum MemberStatus {
        PENDING,
        APPROVED
    }
}
