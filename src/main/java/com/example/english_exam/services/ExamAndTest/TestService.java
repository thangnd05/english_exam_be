package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.PartRequest;
import com.example.english_exam.dto.request.TestRequest;
import com.example.english_exam.dto.response.*;
import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.admin.TestAdminResponse;
import com.example.english_exam.dto.response.admin.TestPartAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.dto.response.user.TestPartResponse;
import com.example.english_exam.dto.response.user.TestResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class TestService {
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestPartRepository testPartRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AnswerService answerService;
    private final RoleRepository  roleRepository;
    private final UserRepository userRepository;
    private final CloudinaryService  cloudinaryService;
    private final ExamTypeRepository examTypeRepository;
    private final PassageRepository  passageRepository;
    private final UserTestRepository userTestRepository;



    public List<Test> getAllTests() {
        return testRepository.findAll();
    }

    public Optional<Test> getTestById(Long id) {
        return testRepository.findById(id);
    }

    public Test save(Test test) {
        return testRepository.save(test);
    }


    public void deleteTest(Long id) {
        testRepository.deleteById(id);
    }

    public TestResponse createTest(TestRequest request, MultipartFile bannerFile) throws IOException {
        ExamType examType = examTypeRepository.findById(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(request.getCreateBy());
        test.setCreatedAt(LocalDateTime.now());
        test.setDurationMinutes(request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : examType.getDurationMinutes());
        test.setAvailableFrom(request.getAvailableFrom());
        test.setAvailableTo(request.getAvailableTo());
        test.setMaxAttempts(request.getMaxAttempts());

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        // ✅ SỬA ĐỔI: Gán trạng thái ban đầu khi tạo. Logic này vẫn đúng.
        test = testRepository.save(test);

        List<TestPartResponse> partResponses = new ArrayList<>();
        for (PartRequest partReq : request.getParts()) {
            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getNumQuestions());
            testPart = testPartRepository.save(testPart);

            List<Question> questions = questionRepository.findRandomByExamPart(
                    partReq.getExamPartId(), partReq.getNumQuestions());

            List<QuestionResponse> questionResponses = new ArrayList<>();
            for (Question q : questions) {
                boolean alreadyLinked = testQuestionRepository
                        .existsByQuestionIdAndTestPartId(q.getQuestionId(), testPart.getTestPartId());
                if (!alreadyLinked) {
                    TestQuestion tq = new TestQuestion();
                    tq.setTestPartId(testPart.getTestPartId());
                    tq.setQuestionId(q.getQuestionId());
                    testQuestionRepository.save(tq);
                }

                PassageResponse passageResponse = null;
                if (q.getPassageId() != null) {
                    Passage passage = passageRepository.findById(q.getPassageId()).orElse(null);
                    if (passage != null) {
                        passageResponse = new PassageResponse(
                                passage.getPassageId(),
                                passage.getContent(),
                                passage.getMediaUrl(),
                                passage.getPassageType().name()
                        );
                    }
                }

                List<AnswerResponse> answerResponses = answerService.getAnswersByQuestionId(q.getQuestionId());

                QuestionResponse qr = new QuestionResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        testPart.getTestPartId(),
                        passageResponse,
                        answerResponses
                );
                questionResponses.add(qr);
            }

            partResponses.add(new TestPartResponse(
                    testPart.getTestPartId(),
                    testPart.getExamPartId(),
                    testPart.getNumQuestions(),
                    questionResponses
            ));
        }

        return new TestResponse(
                test.getTestId(),
                test.getTitle(),
                test.getDescription(),
                test.getExamTypeId(),
                test.getCreatedBy(),
                test.getCreatedAt(),
                test.getBannerUrl(),
                test.getDurationMinutes(),
                test.getAvailableFrom(),
                test.getAvailableTo(),
                test.calculateStatus().name(), // ✅ luôn tính lại
                test.getMaxAttempts(),
                0,
                test.getMaxAttempts(),
                partResponses
        );
    }

    public List<Test> getAllTestsByAdmin() {
        Role adminRole = roleRepository.findByRoleName("Admin");
        if (adminRole == null) return new ArrayList<>();

        List<User> adminUsers = userRepository.findByRoleId(adminRole.getRoleId());
        if (adminUsers.isEmpty()) return new ArrayList<>();

        List<Test> result = new ArrayList<>();
        for (User admin : adminUsers) {
            result.addAll(testRepository.findByCreatedBy(admin.getUserId()));
        }
        return result;
    }

    public List<Test> getTestsByUser(Long userId) {
        return testRepository.findByCreatedBy(userId);
    }

    public TestResponse getTestFullById(Long testId, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        TestStatus currentStatus = test.calculateStatus();

        // Đếm số lần user đã làm bài test này
        int attemptsUsed = userTestRepository.countByUserIdAndTestId(userId, testId);

        // Xử lý maxAttempts null = không giới hạn
        Integer maxAttempts = test.getMaxAttempts(); // có thể null
        Integer remaining = null; // mặc định null = không giới hạn
        if (maxAttempts != null) {
            remaining = Math.max(0, maxAttempts - attemptsUsed);
        }

        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {
            List<TestQuestion> tqList = testQuestionRepository.findByTestPartId(tp.getTestPartId());

            List<QuestionResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                if (q == null) return null;

                PassageResponse passageResponse = null;
                if (q.getPassageId() != null) {
                    Passage passage = passageRepository.findById(q.getPassageId()).orElse(null);
                    if (passage != null) {
                        passageResponse = new PassageResponse(
                                passage.getPassageId(),
                                passage.getContent(),
                                passage.getMediaUrl(),
                                passage.getPassageType().name()
                        );
                    }
                }

                List<AnswerResponse> answers = answerService.getAnswersByQuestionId(q.getQuestionId());
                return new QuestionResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        tp.getTestPartId(),
                        passageResponse,
                        answers
                );
            }).filter(Objects::nonNull).toList();

            return new TestPartResponse(
                    tp.getTestPartId(),
                    tp.getExamPartId(),
                    tp.getNumQuestions(),
                    questionResponses
            );
        }).toList();

        return new TestResponse(
                test.getTestId(),
                test.getTitle(),
                test.getDescription(),
                test.getExamTypeId(),
                test.getCreatedBy(),
                test.getCreatedAt(),
                test.getBannerUrl(),
                test.getDurationMinutes(),
                test.getAvailableFrom(),
                test.getAvailableTo(),
                currentStatus.name(),
                maxAttempts,
                attemptsUsed,
                remaining,
                partResponses
        );
    }


    public TestAdminResponse getTestFullByIdAdmin(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

        List<TestPartAdminResponse> partResponses = testParts.stream().map(tp -> {
            List<TestQuestion> tqList = testQuestionRepository.findByTestPartId(tp.getTestPartId());
            List<QuestionAdminResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                if (q == null) return null;
                PassageResponse passageResponse = null;
                if (q.getPassageId() != null) {
                    Passage passage = passageRepository.findById(q.getPassageId()).orElse(null);
                    if (passage != null) {
                        passageResponse = new PassageResponse(
                                passage.getPassageId(),
                                passage.getContent(),
                                passage.getMediaUrl(),
                                passage.getPassageType().name()
                        );
                    }
                }
                List<AnswerAdminResponse> answers = answerService.getAnswersByQuestionIdForAdmin(q.getQuestionId())
                        .stream()
                        .map(a -> new AnswerAdminResponse(
                                a.getAnswerId(),
                                a.getAnswerText(),
                                a.getIsCorrect(),
                                a.getAnswerLabel()
                        ))
                        .toList();

                return new QuestionAdminResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        tp.getTestPartId(),
                        passageResponse,
                        answers
                );
            }).filter(Objects::nonNull).toList();

            return new TestPartAdminResponse(
                    tp.getTestPartId(),
                    tp.getExamPartId(),
                    tp.getNumQuestions(),
                    questionResponses
            );
        }).toList();

        return new TestAdminResponse(
                test.getTestId(),
                test.getTitle(),
                test.getDescription(),
                test.getExamTypeId(),
                test.getCreatedBy(),
                test.getCreatedAt(),
                test.getBannerUrl(),
                test.getDurationMinutes(),
                test.getAvailableFrom(),
                test.getAvailableTo(),
                // ✅ SỬA ĐỔI: Luôn tính toán lại status cho Admin.
                test.calculateStatus().name(),
                test.getMaxAttempts(),
                partResponses
        );
    }


    public Map<String, Object> canStartTest(Long userId, Test test) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();

        if (test.getAvailableFrom() != null && test.getAvailableFrom().isAfter(now)) {
            result.put("canStart", false);
            result.put("message", "Bài kiểm tra chưa bắt đầu");
            return result;
        }

        if (test.getAvailableTo() != null && test.getAvailableTo().isBefore(now)) {
            result.put("canStart", false);
            result.put("message", "Bài kiểm tra đã kết thúc");
            return result;
        }

        int attemptsUsed = userTestRepository.countByUserIdAndTestId(userId, test.getTestId());
        Integer maxAttempts = test.getMaxAttempts();

        if (maxAttempts != null && attemptsUsed >= maxAttempts) {
            result.put("canStart", false);
            result.put("message", "Bạn đã hết số lượt làm bài");
            return result;
        }

        result.put("canStart", true);
        result.put("message", "OK");
        return result;
    }



}