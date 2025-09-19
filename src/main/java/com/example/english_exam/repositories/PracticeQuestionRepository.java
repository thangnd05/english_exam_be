package com.example.english_exam.repositories;

import com.example.english_exam.models.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> { }