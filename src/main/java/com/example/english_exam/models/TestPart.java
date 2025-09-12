package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testPartId;

    @Column(nullable = false)
    private Long testId;

    @Column(nullable = false)
    private Long examPartId;

    @Column(nullable = false)
    private Integer numQuestions;



}
