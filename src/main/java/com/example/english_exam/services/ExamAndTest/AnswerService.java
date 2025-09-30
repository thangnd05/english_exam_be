package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.models.Answer;
import com.example.english_exam.repositories.AnswerRepository;
import org.springframework.stereotype.Service;

import java.util.*;
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

    public Map<Long, List<AnswerResponse>> getAnswersForMultipleQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Answer> allAnswers = answerRepository.findByQuestionIdIn(questionIds);

        return allAnswers.stream()
                .map(ans -> new AnswerResponse(ans.getAnswerId(), ans.getAnswerText(), ans.getAnswerLabel()))
                .collect(Collectors.groupingBy(
                        // Cần một cách để lấy questionId từ AnswerResponse, hoặc sửa lại logic
                        // Giả sử AnswerResponse có getQuestionId()
                        ar -> findQuestionIdForAnswer(allAnswers, ar.getAnswerId())
                ));
    }

    // Hàm helper (cần cải tiến nếu Answer không có questionId)
    private Long findQuestionIdForAnswer(List<Answer> allAnswers, Long answerId) {
        return allAnswers.stream()
                .filter(a -> a.getAnswerId().equals(answerId))
                .findFirst()
                .map(Answer::getQuestionId)
                .orElse(null);
    }

    public Map<Long, List<AnswerAdminResponse>> getAnswersForMultipleQuestionsForAdmin(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Giả sử AnswerRepository có phương thức findByQuestionIdIn
        List<Answer> allAnswers = answerRepository.findByQuestionIdIn(questionIds);

        // Nhóm các câu trả lời theo questionId
        Map<Long, List<Answer>> groupedByQuestionId = allAnswers.stream()
                .collect(Collectors.groupingBy(Answer::getQuestionId));

        // Chuyển đổi sang DTO
        Map<Long, List<AnswerAdminResponse>> result = new HashMap<>();
        for (Map.Entry<Long, List<Answer>> entry : groupedByQuestionId.entrySet()) {
            List<AnswerAdminResponse> dtoList = entry.getValue().stream()
                    .map(a -> new AnswerAdminResponse(a.getAnswerId(), a.getAnswerText(), a.getIsCorrect(), a.getAnswerLabel()))
                    .toList();
            result.put(entry.getKey(), dtoList);
        }
        return result;
    }
}
