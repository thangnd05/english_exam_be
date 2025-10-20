package com.example.english_exam.repositories;

import com.example.english_exam.models.VocabularyAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularyAlbumRepository extends JpaRepository<VocabularyAlbum, Long> {

    List<VocabularyAlbum> findAllByUserId(Long userId);
}
