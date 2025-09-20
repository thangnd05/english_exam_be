package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.models.PracticeAnswer;
import com.example.english_exam.repositories.PracticeAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PracticeAnswerService {
    private final PracticeAnswerRepository repository;

    public PracticeAnswerService(PracticeAnswerRepository repository) {
        this.repository = repository;
    }

    public List<PracticeAnswer> findAll() { return repository.findAll(); }

    public Optional<PracticeAnswer> findById(Long id) { return repository.findById(id); }

    public PracticeAnswer save(PracticeAnswer answer) { return repository.save(answer); }

    public boolean deleteById(Long id) {
        return repository.findById(id).map(a -> {
            repository.delete(a);
            return true;
        }).orElse(false);
    }
}
