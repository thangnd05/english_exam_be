package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examPartId;

    @Column(nullable = false)
    private Long examTypeId; // FK -> exam_types

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_num_questions")
    private Integer defaultNumQuestions;
}
