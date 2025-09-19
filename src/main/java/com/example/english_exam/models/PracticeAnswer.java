package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long practiceQuestionId; // FK -> practice_question.id
    private String correctEnglish;      // đáp án tiếng Anh
    private String correctVietnamese;   // đáp án tiếng Việt
}
