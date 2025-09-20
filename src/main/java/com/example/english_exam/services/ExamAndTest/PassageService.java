package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.Passage;
import com.example.english_exam.repositories.PassageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PassageService {

    private final PassageRepository passageRepository;

    public PassageService(PassageRepository passageRepository) {
        this.passageRepository = passageRepository;
    }

    public List<Passage> findAll() {
        return passageRepository.findAll();
    }

    public Optional<Passage> findById(Long id) {
        return passageRepository.findById(id);
    }

    public Passage save(Passage passage) {
        return passageRepository.save(passage);
    }

    public void deleteById(Long id) {
        passageRepository.deleteById(id);
    }
}
