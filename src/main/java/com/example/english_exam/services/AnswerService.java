package com.example.english_exam.services;

import com.example.english_exam.models.Answer;
import com.example.english_exam.repositories.AnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnswerService {
    private final AnswerRepository answerRepository;

    public AnswerService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }
    public List<Answer> findAll() {
        return answerRepository.findAll();
    }

    public List<Answer> findByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }

    public Optional<Answer> findById(Long id) {
        return answerRepository.findById(id);
    }

    public void deleteById(Long id) {
        answerRepository.deleteById(id);
    }
}
