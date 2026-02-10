package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.ScoringConversion;
import com.example.english_exam.repositories.ScoringConversionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScoringConversionService {

    private final ScoringConversionRepository scoringConversionRepository;

    public ScoringConversionService(ScoringConversionRepository scoringConversionRepository) {
        this.scoringConversionRepository = scoringConversionRepository;
    }

    public List<ScoringConversion> findAll() {
        return scoringConversionRepository.findAll();
    }

    public Optional<ScoringConversion> findById(Long id) {
        return scoringConversionRepository.findById(id);
    }

    public ScoringConversion save(ScoringConversion conversion) {
        return scoringConversionRepository.save(conversion);
    }

    public void delete(Long id) {
        scoringConversionRepository.deleteById(id);
    }

//    public Optional<ScoringConversion> findConversion(Long examTypeId, Long skillId, Integer numCorrect) {
//        return scoringConversionRepository.findByExamTypeIdAndSkillIdAndNumCorrect(examTypeId, skillId, numCorrect);
//    }
}
