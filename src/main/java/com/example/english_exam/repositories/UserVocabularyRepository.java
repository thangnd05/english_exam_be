package com.example.english_exam.repositories;

import com.example.english_exam.models.UserVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
}
