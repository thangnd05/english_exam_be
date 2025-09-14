package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long skillId;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // Listening, Reading, Speaking, Writing

    @Column(columnDefinition = "TEXT")
    private String description;
}
