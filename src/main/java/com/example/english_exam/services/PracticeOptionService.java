package com.example.english_exam.services;

import com.example.english_exam.models.PracticeOption;
import com.example.english_exam.repositories.PracticeOptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PracticeOptionService {
    private final PracticeOptionRepository repository;

    public PracticeOptionService(PracticeOptionRepository repository) {
        this.repository = repository;
    }

    public List<PracticeOption> findAll() { return repository.findAll(); }

    public Optional<PracticeOption> findById(Long id) { return repository.findById(id); }

    public PracticeOption save(PracticeOption option) { return repository.save(option); }

    public boolean deleteById(Long id) {
        return repository.findById(id).map(o -> {
            repository.delete(o);
            return true;
        }).orElse(false);
    }
}
