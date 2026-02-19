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

    // ✅ Bổ sung các hàm có lọc theo classId
    @Query(value = "SELECT * FROM questions WHERE exam_part_id = :examPartId AND class_id = :classId ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Question findOneRandomQuestionByClass(@Param("examPartId") Long examPartId, @Param("classId") Long classId);

    @Query("SELECT q FROM Question q WHERE q.examPartId = :examPartId AND q.classId = :classId ORDER BY function('RAND')")
    List<Question> findRandomQuestionsByExamPartIdAndClassId(
            @Param("examPartId") Long examPartId,
            @Param("classId") Long classId,
            Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.passageId = :passageId AND q.classId = :classId")
    List<Question> findByPassageIdAndClassId(@Param("passageId") Long passageId, @Param("classId") Long classId);


    @Query("SELECT q FROM Question q WHERE q.examPartId = :examPartId AND q.classId = :classId")
    List<Question> findByExamPartIdAndClassId(@Param("examPartId") Long examPartId, @Param("classId") Long classId);

    @Query("SELECT q FROM Question q WHERE q.examPartId = :examPartId AND q.classId = :classId AND q.chapterId = :chapterId")
    List<Question> findByExamPartIdAndClassIdAndChapterId(
            @Param("examPartId") Long examPartId,
            @Param("classId") Long classId,
            @Param("chapterId") Long chapterId);

    long countByExamPartIdAndClassIdAndChapterId(Long examPartId, Long classId, Long chapterId);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.examPartId = :examPartId AND q.classId = :classId")
    long countByExamPartIdAndClassId(@Param("examPartId") Long examPartId,
                                     @Param("classId") Long classId);


    @Query("""
        SELECT q FROM Question q
        WHERE q.examPartId = :examPartId
          AND q.classId = :classId
          AND q.chapterId = :chapterId
        ORDER BY function('RAND')
    """)
    List<Question> findRandomQuestionsByExamPartIdAndClassIdAndChapterId(
            Long examPartId,
            Long classId,
            Long chapterId,
            Pageable pageable
    );

    // ========== Cá nhân theo user đăng nhập (created_by = userId, class_id/chapter_id NULL) ==========
    List<Question> findByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNull(
            Long examPartId, Long createdBy);

    long countByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNull(
            Long examPartId, Long createdBy);

    @Query(value = """
        SELECT * FROM questions
        WHERE exam_part_id = :examPartId AND created_by = :createdBy
          AND class_id IS NULL AND chapter_id IS NULL
        ORDER BY RAND() LIMIT :limit
        """, nativeQuery = true)
    List<Question> findRandomByExamPartAndCreatedByAndClassIdIsNullAndChapterIdIsNull(
            @Param("examPartId") Long examPartId,
            @Param("createdBy") Long createdBy,
            @Param("limit") int limit);
}
