package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.ExamPart;
import com.example.english_exam.repositories.ExamPartRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExamPartService {
    private final ExamPartRepository examPartRepository;

    public ExamPartService(ExamPartRepository examPartRepository) {
        this.examPartRepository = examPartRepository;
    }

    public List<ExamPart> findAll() {
        return examPartRepository.findAll();
    }

    public List<ExamPart> findByExamTypeId(Long examTypeId) {
        return examPartRepository.findByExamTypeId(examTypeId);
    }

    public Optional<ExamPart> findById(Long id) {
        return examPartRepository.findById(id);
    }

    public ExamPart save(ExamPart examPart) {
        return examPartRepository.save(examPart);
    }

    public void deleteById(Long id) {
        examPartRepository.deleteById(id);
    }
}
