package com.example.english_exam.repositories;

import com.example.english_exam.models.ScoringConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ScoringConversionRepository extends JpaRepository<ScoringConversion, Long> {
    Optional<ScoringConversion> findByExamTypeIdAndSkillIdAndNumCorrect(Long examTypeId, Long skillId, Integer numCorrect);
    List<ScoringConversion> findByExamTypeIdAndSkillId(Long examTypeId, Long skillId);
}
