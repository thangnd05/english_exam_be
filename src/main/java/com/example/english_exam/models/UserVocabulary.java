package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vocabulary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // FK -> users.user_id

    @Column(nullable = false)
    private Long vocabId; // FK -> vocabulary.vocab_id

    @Enumerated(EnumType.STRING)
    private Status status = Status.learning;

    private LocalDateTime lastReviewed = LocalDateTime.now();

    public enum Status {
        learning,
        mastered
    }
}
