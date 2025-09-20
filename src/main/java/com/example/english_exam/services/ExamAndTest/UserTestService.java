package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserTestService {
    private static final Logger log = LoggerFactory.getLogger(UserTestService.class);

    // --- Các repository đã có ---
    private final UserTestRepository userTestRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final AnswerRepository answerRepository;
    private final TestRepository testRepository;
    private final ExamTypeRepository examTypeRepository;
    private final ScoringConversionRepository scoringConversionRepository;
    private final QuestionRepository questionRepository;
    private final ExamPartRepository examPartRepository;

    public UserTestService(UserTestRepository userTestRepository,
                           UserAnswerRepository userAnswerRepository,
                           AnswerRepository answerRepository,
                           TestRepository testRepository,
                           ExamTypeRepository examTypeRepository,
                           ScoringConversionRepository scoringConversionRepository,
                           QuestionRepository questionRepository,
                           ExamPartRepository examPartRepository) {
        this.userTestRepository = userTestRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.answerRepository = answerRepository;
        this.testRepository = testRepository;
        this.examTypeRepository = examTypeRepository;
        this.scoringConversionRepository = scoringConversionRepository;
        this.questionRepository = questionRepository;
        this.examPartRepository = examPartRepository;
    }

    @Transactional
    public UserTest submitTest(Long userTestId) {
        log.debug("Submitting test with UserTestId={}", userTestId);

        UserTest userTest = userTestRepository.findById(userTestId)
                .orElseThrow(() -> new RuntimeException("UserTest not found"));

        userTest.setFinishedAt(LocalDateTime.now());

        List<UserAnswer> userAnswers = userAnswerRepository.findByUserTestId(userTestId);
        if (userAnswers.isEmpty()) {
            log.debug("No user answers found, totalScore=0");
            userTest.setTotalScore(0);
            return userTestRepository.save(userTest);
        }

        Test test = testRepository.findById(userTest.getTestId())
                .orElseThrow(() -> new RuntimeException("Test not found"));

        ExamType examType = examTypeRepository.findById(test.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        String scoringMethod = examType.getScoringMethod() != null ? examType.getScoringMethod().toLowerCase() : "default";

        log.debug("DEBUG: Scoring method for ExamType ID " + examType.getExamTypeId() + " is '" + scoringMethod + "'");

        int totalScore;
        if ("toeic_scale".equalsIgnoreCase(scoringMethod)) {
            totalScore = scoreToeicOptimal(userAnswers, test, examType);
        } else {
            totalScore = scoreDefault(userAnswers);
        }

        log.debug("Total score for UserTestId={} is {}", userTestId, totalScore);
        userTest.setTotalScore(totalScore);
        return userTestRepository.save(userTest);
    }

    private int scoreDefault(List<UserAnswer> userAnswers) {
        List<Long> questionIds = userAnswers.stream().map(UserAnswer::getQuestionId).toList();
        Map<Long, Long> correctMap = answerRepository.findByQuestionIdInAndIsCorrectTrue(questionIds).stream()
                .collect(Collectors.toMap(Answer::getQuestionId, Answer::getAnswerId));

        return (int) userAnswers.stream()
                .filter(ua -> ua.getSelectedAnswerId() != null && ua.getSelectedAnswerId().equals(correctMap.get(ua.getQuestionId())))
                .count();
    }

    private int scoreToeicOptimal(List<UserAnswer> userAnswers, Test test, ExamType examType) {
        log.debug("===== START TOEIC SCORING DEBUG =====");

        // 1. Lấy tất cả ID câu hỏi và ID đáp án đúng
        List<Long> allQuestionIds = userAnswers.stream().map(UserAnswer::getQuestionId).toList();
        Map<Long, Long> correctAnswersMap = answerRepository.findByQuestionIdInAndIsCorrectTrue(allQuestionIds)
                .stream()
                .collect(Collectors.toMap(Answer::getQuestionId, Answer::getAnswerId));

        // 2. Lấy thông tin Question để lấy examPartId
        List<Question> questions = questionRepository.findAllById(allQuestionIds);
        Map<Long, Long> questionToExamPartIdMap = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, Question::getExamPartId));

        // 3. Lấy thông tin ExamPart để lấy skillId
        Set<Long> allExamPartIds = questions.stream().map(Question::getExamPartId).collect(Collectors.toSet());
        List<ExamPart> examParts = examPartRepository.findAllById(allExamPartIds);
        Map<Long, Long> examPartToSkillIdMap = examParts.stream()
                .collect(Collectors.toMap(ExamPart::getExamPartId, ExamPart::getSkillId));

        Map<Long, Integer> skillCorrectCount = new HashMap<>();

        for (UserAnswer ua : userAnswers) {
            Long questionId = ua.getQuestionId();
            Long selectedAnswerId = ua.getSelectedAnswerId();
            Long correctAnswerId = correctAnswersMap.get(questionId);
            Long examPartId = questionToExamPartIdMap.get(questionId);
            Long skillId = examPartId != null ? examPartToSkillIdMap.get(examPartId) : null;

            boolean isCorrect = selectedAnswerId != null && selectedAnswerId.equals(correctAnswerId);
            if (isCorrect && skillId != null) {
                skillCorrectCount.merge(skillId, 1, Integer::sum);
            }

            log.debug("QuestionId: {}, Selected: {}, Correct: {}, ExamPartId: {}, SkillId: {}, IsCorrect: {}",
                    questionId, selectedAnswerId, correctAnswerId, examPartId, skillId, isCorrect);
        }

        // Quy đổi điểm
        int totalScore = 0;
        for (Map.Entry<Long, Integer> entry : skillCorrectCount.entrySet()) {
            Long skillId = entry.getKey();
            Integer numCorrect = entry.getValue();

            int convertedScore = scoringConversionRepository
                    .findByExamTypeIdAndSkillIdAndNumCorrect(examType.getExamTypeId(), skillId, numCorrect)
                    .map(ScoringConversion::getConvertedScore)
                    .orElse(5);

            totalScore += convertedScore;

            log.debug("SkillId: {}, NumCorrect: {}, ConvertedScore: {}", skillId, numCorrect, convertedScore);
        }

        // Xử lý trường hợp skill không đúng câu nào
        Set<Long> allSkillIdsInTest = examParts.stream().map(ExamPart::getSkillId).collect(Collectors.toSet());
        for (Long skillId : allSkillIdsInTest) {
            if (!skillCorrectCount.containsKey(skillId)) {
                int convertedScore = scoringConversionRepository
                        .findByExamTypeIdAndSkillIdAndNumCorrect(examType.getExamTypeId(), skillId, 0)
                        .map(ScoringConversion::getConvertedScore)
                        .orElse(5);
                totalScore += convertedScore;

                log.debug("SkillId: {}, NumCorrect: 0, ConvertedScore (0 correct): {}", skillId, convertedScore);
            }
        }

        log.debug("TotalScore: {}", totalScore);
        log.debug("===== END TOEIC SCORING DEBUG =====");
        return totalScore;
    }

    // --- Các phương thức khác giữ nguyên ---
    public List<UserTest> findAll() { return userTestRepository.findAll(); }
    public Optional<UserTest> findById(Long id) { return userTestRepository.findById(id); }
    public List<UserTest> findByUserId(Long userId) { return userTestRepository.findByUserId(userId); }
    public List<UserTest> findByTestId(Long testId) { return userTestRepository.findByTestId(testId); }
    public UserTest save(UserTest userTest) { return userTestRepository.save(userTest); }
    public boolean delete(Long id) {
        return userTestRepository.findById(id).map(u -> {
            userTestRepository.delete(u);
            return true;
        }).orElse(false);
    }
}
