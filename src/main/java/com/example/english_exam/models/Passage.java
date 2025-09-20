package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Passage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passageId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 255)
    private String mediaUrl; // có thể null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassageType passageType; // thêm trường này

    public enum PassageType {
        READING,
        LISTENING
    }
}

