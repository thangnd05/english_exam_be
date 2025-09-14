package com.example.english_exam.repositories;

import com.example.english_exam.models.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    List<TestQuestion> findByTestPartId(Long testPartId);
    List<TestQuestion> findByQuestionId(Long questionId);
}
