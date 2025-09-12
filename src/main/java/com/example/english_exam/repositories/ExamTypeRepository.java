package com.example.english_exam.repositories;

import com.example.english_exam.models.ExamTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamTypes, Long> {
}
