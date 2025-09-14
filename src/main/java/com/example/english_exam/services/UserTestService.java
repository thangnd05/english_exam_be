package com.example.english_exam.services;

import com.example.english_exam.models.UserTest;
import com.example.english_exam.repositories.UserTestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserTestService {
    private final UserTestRepository userTestRepository;

    public UserTestService(UserTestRepository userTestRepository) {
        this.userTestRepository = userTestRepository;
    }

    public List<UserTest> findAll() {
        return userTestRepository.findAll();
    }

    public Optional<UserTest> findById(Long id) {
        return userTestRepository.findById(id);
    }

    public List<UserTest> findByUserId(Long userId) {
        return userTestRepository.findByUserId(userId);
    }

    public List<UserTest> findByTestId(Long testId) {
        return userTestRepository.findByTestId(testId);
    }

    public UserTest save(UserTest userTest) {
        return userTestRepository.save(userTest);
    }

    public boolean delete(Long id) {
        return userTestRepository.findById(id).map(u -> {
            userTestRepository.delete(u);
            return true;
        }).orElse(false);
    }
}
