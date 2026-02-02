package com.example.english_exam.repositories;

import com.example.english_exam.models.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // Lấy tất cả đánh giá theo user
    List<Evaluation> findByUserId(Long userId);
}
