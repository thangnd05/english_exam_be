package com.example.english_exam.repositories;

import com.example.english_exam.models.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // Lấy tất cả đáp án của 1 câu hỏi
    List<Answer> findByQuestionId(Long questionId);

    // Lấy đáp án đúng duy nhất của 1 câu hỏi
    Optional<Answer> findByQuestionIdAndIsCorrectTrue(Long questionId);

    // Lấy tất cả đáp án đúng của nhiều câu hỏi
    List<Answer> findByQuestionIdInAndIsCorrectTrue(List<Long> questionIds);

    // Optional: kiểm tra đáp án theo question và answerId
    Optional<Answer> findByQuestionIdAndAnswerId(Long questionId, Long answerId);

    List<Answer> findByQuestionIdIn(List<Long> questionIds);

    void deleteByQuestionId(Long questionId);
}
