package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.models.Answer;
import com.example.english_exam.repositories.AnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<AnswerResponse> getAnswersByQuestionId(Long questionId) {
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        return answers.stream()
                .map(ans -> new AnswerResponse(
                        ans.getAnswerId(),
                        ans.getAnswerText(),
                        ans.getAnswerLabel()
                ))
                .collect(Collectors.toList());
    }

    public List<AnswerAdminResponse> getAnswersByQuestionIdForAdmin(Long questionId) {
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        return answers.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),
                        a.getAnswerLabel()
                ))
                .toList();
    }
}
