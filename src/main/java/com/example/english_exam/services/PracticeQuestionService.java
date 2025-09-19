package com.example.english_exam.services;

import com.example.english_exam.models.PracticeQuestion;
import com.example.english_exam.repositories.PracticeQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PracticeQuestionService {
    private final PracticeQuestionRepository repository;

    public PracticeQuestionService(PracticeQuestionRepository repository) {
        this.repository = repository;
    }

    public List<PracticeQuestion> findAll() { return repository.findAll(); }

    public Optional<PracticeQuestion> findById(Long id) { return repository.findById(id); }

    public PracticeQuestion save(PracticeQuestion question) { return repository.save(question); }

    public boolean deleteById(Long id) {
        return repository.findById(id).map(q -> {
            repository.delete(q);
            return true;
        }).orElse(false);
    }
}
