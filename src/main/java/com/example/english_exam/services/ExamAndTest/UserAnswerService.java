package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.UserAnswer;
import com.example.english_exam.repositories.UserAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserAnswerService {
    private final UserAnswerRepository userAnswerRepository;

    public UserAnswerService(UserAnswerRepository userAnswerRepository) {
        this.userAnswerRepository = userAnswerRepository;
    }

    public List<UserAnswer> findAll() {
        return userAnswerRepository.findAll();
    }

    public Optional<UserAnswer> findById(Long id) {
        return userAnswerRepository.findById(id);
    }

    public List<UserAnswer> findByUserTestId(Long userTestId) {
        return userAnswerRepository.findByUserTestId(userTestId);
    }

    public List<UserAnswer> findByQuestionId(Long questionId) {
        return userAnswerRepository.findByQuestionId(questionId);
    }

    public UserAnswer save(UserAnswer userAnswer) {
        return userAnswerRepository.save(userAnswer);
    }

    public boolean delete(Long id) {
        return userAnswerRepository.findById(id).map(u -> {
            userAnswerRepository.delete(u);
            return true;
        }).orElse(false);
    }
}
