package com.example.english_exam.repositories;

import com.example.english_exam.models.TestPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPartRepository extends JpaRepository<TestPart, Long> {
    List<TestPart> findByTestId(Long testId);
    List<TestPart> findByExamPartId(Long examPartId);
}
