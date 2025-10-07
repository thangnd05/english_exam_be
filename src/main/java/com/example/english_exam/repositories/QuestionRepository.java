package com.example.english_exam.repositories;

import com.example.english_exam.models.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;


import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamPartId(Long examPartId);
    List<Question> findByPassageId(Long passageId);

    @Query(value = "SELECT * FROM questions WHERE exam_part_id = :examPartId ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomByExamPart(@Param("examPartId") Long examPartId, @Param("limit") int limit);

    @Query("SELECT q FROM Question q WHERE q.examPartId = :examPartId ORDER BY function('RAND')")
    List<Question> findRandomQuestionsByExamPartId(@Param("examPartId") Long examPartId, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.examPartId = :examPartId")
    long countByExamPartId(@Param("examPartId") Long examPartId);

    // Random 1 câu (để kiểm tra có passage hay không)
    @Query(value = "SELECT * FROM questions WHERE exam_part_id = :examPartId ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Question findOneRandomQuestion(@Param("examPartId") Long examPartId);
}
