package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.response.ResultSummaryDto;
import com.example.english_exam.models.Answer;
import com.example.english_exam.models.Question;
import com.example.english_exam.models.UserAnswer;
import com.example.english_exam.repositories.AnswerRepository;
import com.example.english_exam.repositories.QuestionRepository;
import com.example.english_exam.repositories.UserAnswerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserAnswerService {
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;




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


    // ✅ PHƯƠNG THỨC LOGIC MỚI ĐƯỢC CHUYỂN VÀO ĐÂY
    public ResultSummaryDto getResultSummary(Long userTestId) {
        List<UserAnswer> userAnswers = userAnswerRepository.findByUserTestId(userTestId);
        if (userAnswers.isEmpty()) {
            return new ResultSummaryDto(0, 0, 0);
        }

        Set<Long> questionIds = userAnswers.stream()
                .map(UserAnswer::getQuestionId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Map<Long, Answer> correctAnswersMap = answerRepository.findByQuestionIdInAndIsCorrectTrue(new ArrayList<>(questionIds))
                .stream()
                .collect(Collectors.toMap(Answer::getQuestionId, answer -> answer));

        long correctCount = 0;
        for (UserAnswer userAnswer : userAnswers) {
            Long qId = userAnswer.getQuestionId();
            Question question = questionMap.get(qId);
            Answer correctAnswer = correctAnswersMap.get(qId);

            if (question == null || correctAnswer == null) continue;

            boolean isCorrect = false;
            if (question.getQuestionType() == Question.QuestionType.MCQ) {
                isCorrect = userAnswer.getSelectedAnswerId() != null &&
                        userAnswer.getSelectedAnswerId().equals(correctAnswer.getAnswerId());
            } else if (question.getQuestionType() == Question.QuestionType.FILL_BLANK) {
                isCorrect = userAnswer.getAnswerText() != null &&
                        userAnswer.getAnswerText().trim().equalsIgnoreCase(correctAnswer.getAnswerText().trim());
            }

            if (isCorrect) {
                correctCount++;
            }
        }

        long totalQuestions = userAnswers.size();
        long wrongCount = totalQuestions - correctCount;

        return new ResultSummaryDto(correctCount, wrongCount, totalQuestions);
    }
}
