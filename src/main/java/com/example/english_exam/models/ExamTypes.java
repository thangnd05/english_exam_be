package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examTypeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
