package com.example.english_exam.repositories;

import com.example.english_exam.models.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test>findByCreatedBy(Long id);

    @Query("SELECT t FROM Test t " +
            "LEFT JOIN FETCH t.testParts tp " +
            "LEFT JOIN FETCH tp.examPart ep " +
            "LEFT JOIN FETCH tp.testQuestions tq " +
            "LEFT JOIN FETCH tq.question q " +
            "LEFT JOIN FETCH q.passage p " +
            "LEFT JOIN FETCH q.answers a " +
            "WHERE t.testId = :testId")
    Test findFullTestById(@Param("testId") Long testId);
}
