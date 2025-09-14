package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scoring_conversion",
        uniqueConstraints = @UniqueConstraint(name = "uk_scoring", columnNames = {"exam_type_id", "skill_id", "num_correct"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConversion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversionId;

    @Column(nullable = false)
    private Long examTypeId; // FK -> exam_types

    @Column(nullable = false)
    private Long skillId; // FK -> skills

    @Column(nullable = false)
    private Integer numCorrect;

    @Column(nullable = false)
    private Integer convertedScore;
}
