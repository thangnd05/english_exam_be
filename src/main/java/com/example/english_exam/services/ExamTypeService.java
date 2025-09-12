package com.example.english_exam.services;

import com.example.english_exam.models.ExamTypes;
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

    public List<ExamTypes> findAll() {
        return examTypeRepository.findAll();
    }

    public Optional<ExamTypes> findById(Long id) {
        return examTypeRepository.findById(id);
    }

    public ExamTypes save(ExamTypes examType) {
        return examTypeRepository.save(examType);
    }

    public void deleteById(Long id) {
        examTypeRepository.deleteById(id);
    }
}
