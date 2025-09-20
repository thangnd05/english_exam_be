package com.example.english_exam.repositories;

import com.example.english_exam.models.PracticeAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, Long> {
    Optional<PracticeAnswer> findByPracticeQuestionId(Long questionId);

}
