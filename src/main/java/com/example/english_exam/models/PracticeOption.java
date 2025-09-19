package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long practiceQuestionId; // FK -> practice_question.id
    private String optionText;
    private boolean isCorrect = false;
}
