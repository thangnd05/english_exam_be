package com.example.english_exam.repositories;

import com.example.english_exam.models.Test;
import com.example.english_exam.models.TestPart;
import com.example.english_exam.models.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test>findByCreatedBy(Long id);

    public interface TestPartRepository extends JpaRepository<TestPart, Long> {
        List<TestPart> findByTestId(Long testId);
    }

    @Repository
    public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
        List<TestQuestion> findByTestPartId(Long testPartId);
    }

}
