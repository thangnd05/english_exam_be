package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary_album")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyAlbum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long albumId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(nullable = false)
    private Long userId; // FK -> users.user_id

    private LocalDateTime createdAt = LocalDateTime.now();
}
