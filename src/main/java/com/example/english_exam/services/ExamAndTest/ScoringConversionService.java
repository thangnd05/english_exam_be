package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.request.ScoringConversionRequest;
import com.example.english_exam.dto.response.ScoringConversionResponse;
import com.example.english_exam.models.ScoringConversion;
import com.example.english_exam.repositories.ScoringConversionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ScoringConversionService {

    private final ScoringConversionRepository scoringConversionRepository;

    public List<ScoringConversionResponse> findAll() {
        return scoringConversionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ScoringConversionResponse findById(Long id) {
        ScoringConversion c = scoringConversionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scoring conversion không tồn tại"));
        return toResponse(c);
    }

    public ScoringConversionResponse create(ScoringConversionRequest request) {
        ScoringConversion c = new ScoringConversion();
        c.setExamTypeId(request.getExamTypeId());
        c.setSkillId(request.getSkillId());
        c.setNumCorrect(request.getNumCorrect());
        c.setConvertedScore(request.getConvertedScore());
        c = scoringConversionRepository.save(c);
        return toResponse(c);
    }

    public ScoringConversionResponse update(Long id, ScoringConversionRequest request) {
        ScoringConversion c = scoringConversionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scoring conversion không tồn tại"));
        if (request.getExamTypeId() != null) c.setExamTypeId(request.getExamTypeId());
        if (request.getSkillId() != null) c.setSkillId(request.getSkillId());
        if (request.getNumCorrect() != null) c.setNumCorrect(request.getNumCorrect());
        if (request.getConvertedScore() != null) c.setConvertedScore(request.getConvertedScore());
        c = scoringConversionRepository.save(c);
        return toResponse(c);
    }

    public void delete(Long id) {
        ScoringConversion c = scoringConversionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scoring conversion không tồn tại"));
        scoringConversionRepository.delete(c);
    }

    private ScoringConversionResponse toResponse(ScoringConversion c) {
        ScoringConversionResponse res = new ScoringConversionResponse();
        res.setConversionId(c.getConversionId());
        res.setExamTypeId(c.getExamTypeId());
        res.setSkillId(c.getSkillId());
        res.setNumCorrect(c.getNumCorrect());
        res.setConvertedScore(c.getConvertedScore());
        return res;
    }
}
