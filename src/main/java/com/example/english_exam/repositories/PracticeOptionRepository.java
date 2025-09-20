package com.example.english_exam.repositories;

import com.example.english_exam.models.PracticeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PracticeOptionRepository extends JpaRepository<PracticeOption, Long> {
    List<PracticeOption> findByPracticeQuestionId(Long questionId);

}