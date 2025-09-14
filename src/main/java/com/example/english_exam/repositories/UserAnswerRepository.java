package com.example.english_exam.repositories;

import com.example.english_exam.models.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByUserTestId(Long userTestId);
    List<UserAnswer> findByQuestionId(Long questionId);
}
