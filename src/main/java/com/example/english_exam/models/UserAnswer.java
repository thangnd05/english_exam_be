package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAnswerId;

    @Column(nullable = false)
    private Long userTestId; // FK -> user_tests.user_test_id

    @Column(nullable = false)
    private Long questionId; // FK -> questions.question_id

    private Long selectedAnswerId; // FK -> answers.answer_id (null nếu tự luận)

    @Column(columnDefinition = "TEXT")
    private String answerText; // cho tự luận

    private Boolean isCorrect;

    private Integer score = 0;

    @Column(nullable = false)
    private LocalDateTime answeredAt = LocalDateTime.now();
}
