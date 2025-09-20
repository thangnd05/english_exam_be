package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.TestPart;
import com.example.english_exam.repositories.TestPartRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestPartService {
    private final TestPartRepository testPartRepository;

    public TestPartService(TestPartRepository testPartRepository) {
        this.testPartRepository = testPartRepository;
    }

    public List<TestPart> findAll() {
        return testPartRepository.findAll();
    }

    public List<TestPart> findByTestId(Long testId) {
        return testPartRepository.findByTestId(testId);
    }

    public Optional<TestPart> findById(Long id) {
        return testPartRepository.findById(id);
    }

    public TestPart save(TestPart testPart) {
        return testPartRepository.save(testPart);
    }

    public void deleteById(Long id) {
        testPartRepository.deleteById(id);
    }
}
