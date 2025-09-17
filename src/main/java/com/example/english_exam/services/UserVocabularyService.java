package com.example.english_exam.services;

import com.example.english_exam.models.UserVocabulary;
import com.example.english_exam.repositories.UserVocabularyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserVocabularyService {
    private final UserVocabularyRepository repository;

    public UserVocabularyService(UserVocabularyRepository repository) {
        this.repository = repository;
    }

    public List<UserVocabulary> findAll() {
        return repository.findAll();
    }

    public Optional<UserVocabulary> findById(Long id) {
        return repository.findById(id);
    }

    public UserVocabulary save(UserVocabulary uv) {
        return repository.save(uv);
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(u -> {
            repository.delete(u);
            return true;
        }).orElse(false);
    }
}
