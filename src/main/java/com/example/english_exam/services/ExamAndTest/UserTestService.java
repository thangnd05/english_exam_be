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
        userTest.setStatus(UserTest.Status.COMPLETED); // 🟢 Cập nhật trạng thái đã nộp


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
        log.debug("DEBUG: Scoring method for ExamType ID {} is '{}'", examType.getExamTypeId(), scoringMethod);

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
        if (userAnswers.isEmpty()) {
            return 0;
        }

        // Tạo Set để lấy các ID câu hỏi duy nhất
        Set<Long> questionIds = userAnswers.stream()
                .map(UserAnswer::getQuestionId)
                .collect(Collectors.toSet());

        // Lấy thông tin của các câu hỏi
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Lấy thông tin đáp án đúng đầy đủ
        Map<Long, Answer> correctAnswersMap = answerRepository.findByQuestionIdInAndIsCorrectTrue(new ArrayList<>(questionIds)) // Chuyển Set thành List
                .stream()
                .collect(Collectors.toMap(Answer::getQuestionId, answer -> answer));

        int correctCount = 0;
        for (UserAnswer userAnswer : userAnswers) {
            Long qId = userAnswer.getQuestionId();
            Question question = questionMap.get(qId);
            Answer correctAnswer = correctAnswersMap.get(qId);

            if (question == null || correctAnswer == null) {
                continue;
            }

            boolean isCorrect = false;
            // Kiểm tra đúng/sai dựa trên loại câu hỏi
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
        return correctCount;
    }

    private int scoreToeicOptimal(List<UserAnswer> userAnswers, Test test, ExamType examType) {
        log.debug("===== START TOEIC SCORING DEBUG =====");

        // 1. Lấy thông tin Question đầy đủ
        List<Long> allQuestionIds = userAnswers.stream().map(UserAnswer::getQuestionId).toList();
        List<Question> questions = questionRepository.findAllById(allQuestionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // 2. Lấy thông tin đáp án đúng đầy đủ
        Map<Long, Answer> correctAnswersMap = answerRepository.findByQuestionIdInAndIsCorrectTrue(allQuestionIds)
                .stream()
                .collect(Collectors.toMap(Answer::getQuestionId, answer -> answer));

        // 3. Lấy thông tin ExamPart và Skill
        Set<Long> allExamPartIds = questions.stream().map(Question::getExamPartId).collect(Collectors.toSet());
        List<ExamPart> examParts = examPartRepository.findAllById(allExamPartIds);
        Map<Long, Long> examPartToSkillIdMap = examParts.stream()
                .collect(Collectors.toMap(ExamPart::getExamPartId, ExamPart::getSkillId));

        Map<Long, Integer> skillCorrectCount = new HashMap<>();

        for (UserAnswer ua : userAnswers) {
            Long questionId = ua.getQuestionId();
            Question question = questionMap.get(questionId);
            Answer correctAnswer = correctAnswersMap.get(questionId);

            if (question == null || correctAnswer == null) {
                continue;
            }

            // 4. Logic kiểm tra đúng/sai đã được nâng cấp
            boolean isCorrect = false;
            if (question.getQuestionType() == Question.QuestionType.MCQ) {
                isCorrect = ua.getSelectedAnswerId() != null && ua.getSelectedAnswerId().equals(correctAnswer.getAnswerId());
            } else if (question.getQuestionType() == Question.QuestionType.FILL_BLANK) {
                isCorrect = ua.getAnswerText() != null && ua.getAnswerText().trim().equalsIgnoreCase(correctAnswer.getAnswerText().trim());
            }

            Long examPartId = question.getExamPartId();
            Long skillId = examPartId != null ? examPartToSkillIdMap.get(examPartId) : null;

            if (isCorrect && skillId != null) {
                skillCorrectCount.merge(skillId, 1, Integer::sum);
            }
            log.debug("QuestionId: {}, Type: {}, IsCorrect: {}", questionId, question.getQuestionType(), isCorrect);
        }

        // 5. Quy đổi điểm
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

        Set<Long> allSkillIdsInTest = examParts.stream().map(ExamPart::getSkillId).filter(Objects::nonNull).collect(Collectors.toSet());
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

    // --- Các phương thức CRUD khác ---
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

    @Transactional
    public UserTest startUserTest(Long testId, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));

        Optional<UserTest> existing = userTestRepository.findActiveUserTest(userId, testId, UserTest.Status.IN_PROGRESS);
        if (existing.isPresent()) {
            log.info("✅ Reusing existing UserTest for user {} test {}", userId, testId);
            return existing.get();
        }

        UserTest newTest = new UserTest();
        newTest.setUserId(userId);
        newTest.setTestId(testId);
        newTest.setStartedAt(LocalDateTime.now());
        newTest.setStatus(UserTest.Status.IN_PROGRESS);
        newTest.setTotalScore(0);

        log.info("🆕 Created new UserTest for user {} test {}", userId, testId);
        return userTestRepository.save(newTest);
    }

    public Optional<UserTest> findActiveUserTest(Long userId, Long testId) {
        return userTestRepository.findActiveUserTest(userId, testId, UserTest.Status.IN_PROGRESS);
    }





}