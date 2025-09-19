package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vocabId; // FK -> vocabulary.vocab_id

    @Enumerated(EnumType.STRING)
    private QuestionType type; // MULTICHOICE, LISTENING_EN, LISTENING_VI, WRITING_EN, WRITING_VI

    private String questionText;
    private String audioUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum QuestionType {
        MULTICHOICE,
        LISTENING_EN,
        LISTENING_VI,
        WRITING_EN,
        WRITING_VI
    }
}
