package com.example.english_exam.repositories;

import com.example.english_exam.models.VocabularyAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyAlbumRepository extends JpaRepository<VocabularyAlbum, Long> {
}
