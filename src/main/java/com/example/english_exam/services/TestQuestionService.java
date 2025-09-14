package com.example.english_exam.services;

import com.example.english_exam.models.TestQuestion;
import com.example.english_exam.repositories.TestQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestQuestionService {
    private final TestQuestionRepository testQuestionRepository;

    public TestQuestionService(TestQuestionRepository testQuestionRepository) {
        this.testQuestionRepository = testQuestionRepository;
    }

    public List<TestQuestion> getAllTestQuestions() {
        return testQuestionRepository.findAll();
    }

    public Optional<TestQuestion> getTestQuestionById(Long id) {
        return testQuestionRepository.findById(id);
    }

    public TestQuestion saveTestQuestion(TestQuestion testQuestion) {
        return testQuestionRepository.save(testQuestion);
    }


    public void deleteTestQuestionById(Long id) {
        testQuestionRepository.deleteById(id);
    }
}
