package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(nullable = false)
    private Long examPartId; // FK -> exam_parts

    private Long passageId; // FK -> passages (nullable)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    private Long createdBy;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String explanation; // optional: có thể AI generate

    public enum QuestionType {
        MCQ, FILL_BLANK, ESSAY
    }

    @Column(name = "class_id")
    private Long classId;

    @Column(name ="chapter_id")
    private Long chapterId;
}
