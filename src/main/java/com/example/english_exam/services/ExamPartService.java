package com.example.english_exam.services;

import com.example.english_exam.models.ExamParts;
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

    public List<ExamParts> findAll() {
        return examPartRepository.findAll();
    }

    public List<ExamParts> findByExamTypeId(Long examTypeId) {
        return examPartRepository.findByExamTypeId(examTypeId);
    }

    public Optional<ExamParts> findById(Long id) {
        return examPartRepository.findById(id);
    }

    public ExamParts save(ExamParts examPart) {
        return examPartRepository.save(examPart);
    }

    public void deleteById(Long id) {
        examPartRepository.deleteById(id);
    }
}
