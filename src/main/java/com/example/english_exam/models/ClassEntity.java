package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {

    @Id
    @Column(name = "class_id", nullable = false)
    private Long classId; // bạn sẽ tự sinh random ID trong service

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId; // FK -> users.user_id

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
