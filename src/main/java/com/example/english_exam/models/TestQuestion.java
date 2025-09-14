package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_question_id")
    private Long testQuestionId;

    @Column(name = "test_part_id", nullable = false)
    private Long testPartId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;
}
