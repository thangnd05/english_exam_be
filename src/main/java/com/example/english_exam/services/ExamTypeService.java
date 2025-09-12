package com.example.english_exam.services;

import com.example.english_exam.models.ExamType;
import com.example.english_exam.repositories.ExamTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExamTypeService {
    private final ExamTypeRepository examTypeRepository;

    public ExamTypeService(ExamTypeRepository examTypeRepository) {
        this.examTypeRepository = examTypeRepository;
    }

    public List<ExamType> findAll() {
        return examTypeRepository.findAll();
    }

    public Optional<ExamType> findById(Long id) {
        return examTypeRepository.findById(id);
    }

    public ExamType save(ExamType examType) {
        return examTypeRepository.save(examType);
    }

    public void deleteById(Long id) {
        examTypeRepository.deleteById(id);
    }
}
