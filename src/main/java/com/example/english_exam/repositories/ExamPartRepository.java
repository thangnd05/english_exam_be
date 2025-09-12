package com.example.english_exam.repositories;

import com.example.english_exam.models.ExamParts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamPartRepository extends JpaRepository<ExamParts, Long> {
    List<ExamParts> findByExamTypeId(Long examTypeId);
}
