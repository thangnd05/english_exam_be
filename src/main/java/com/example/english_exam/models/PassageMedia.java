package com.example.english_exam.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "passage_media")
@Data
public class PassageMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long passageId;

    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;



    public enum MediaType {
        IMAGE,
        AUDIO,
        DOCUMENT,
    }
}

