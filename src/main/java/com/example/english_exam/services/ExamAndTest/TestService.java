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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
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

    public TestService(TestRepository testRepository, QuestionRepository questionRepository, TestPartRepository testPartRepository, TestQuestionRepository testQuestionRepository, AnswerService answerService, RoleRepository roleRepository, UserRepository userRepository, CloudinaryService cloudinaryService, ExamTypeRepository examTypeRepository, PassageRepository passageRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testPartRepository = testPartRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.answerService = answerService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.examTypeRepository = examTypeRepository;
        this.passageRepository = passageRepository;
    }

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
        // Lấy examType để lấy default duration
        ExamType examType = examTypeRepository.findById(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        // Tạo test
        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(request.getStudentId());
        test.setCreatedAt(LocalDateTime.now());

        // Duration: custom nếu có, default nếu không
        test.setDurationMinutes(request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : examType.getDurationMinutes());

        // Upload banner nếu có
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        // Lưu test
        test = testRepository.save(test);

        // Tạo parts và random câu hỏi
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

                // Build PassageResponse (nếu có passage)
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


                // Build AnswerResponse list
                List<AnswerResponse> answerResponses = answerService.getAnswersByQuestionId(q.getQuestionId());

                // Build QuestionResponse
                QuestionResponse qr = new QuestionResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),   // enum
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

        // Trả response
        return new TestResponse(
                test.getTestId(),
                test.getTitle(),
                test.getDescription(),
                test.getExamTypeId(),
                test.getCreatedBy(),
                test.getCreatedAt(),
                test.getBannerUrl(),
                test.getDurationMinutes(), // duration thực tế
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

    // Lấy test theo userId cụ thể
    public List<Test> getTestsByUser(Long userId) {
        return testRepository.findByCreatedBy(userId);
    }


    public List<TestResponse> getAllTestsWithDB() {
        List<Test> tests = testRepository.findAll();

        return tests.stream().map(test -> {
            // Lấy parts theo testId
            List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

            List<TestPartResponse> partResponses = testParts.stream().map(tp -> {
                // Lấy danh sách questionId từ test_question
                List<TestQuestion> tqList = testQuestionRepository.findByTestPartId(tp.getTestPartId());

                List<QuestionResponse> questionResponses = tqList.stream().map(tq -> {
                    Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                    if (q == null) return null;

                    // Passage (fix biến final/effectively final)
                    PassageResponse passageResponse = null;
                    if (q.getPassageId() != null) {
                        passageResponse = passageRepository.findById(q.getPassageId())
                                .map(p -> new PassageResponse(
                                        p.getPassageId(),
                                        p.getContent(),
                                        p.getMediaUrl(),
                                        p.getPassageType().name()
                                ))
                                .orElse(null);
                    }

                    // Answers
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
                    partResponses
            );
        }).toList();
    }

    public TestResponse getTestFullById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Lấy parts theo testId
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {
            // Lấy danh sách questionId từ test_question
            List<TestQuestion> tqList = testQuestionRepository.findByTestPartId(tp.getTestPartId());

            List<QuestionResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                if (q == null) return null;

                // Passage
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

                // Answers
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
                partResponses
        );
    }

    public TestAdminResponse getTestFullByIdAdmin(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Lấy parts theo testId
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

        List<TestPartAdminResponse> partResponses = testParts.stream().map(tp -> {
            // Lấy danh sách questionId từ test_question
            List<TestQuestion> tqList = testQuestionRepository.findByTestPartId(tp.getTestPartId());

            List<QuestionAdminResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                if (q == null) return null;

                // Passage
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

                // Answers (Admin: có isCorrect)
                List<AnswerAdminResponse> answers = answerService.getAnswersByQuestionIdForAdmin(q.getQuestionId())
                        .stream()
                        .map(a -> new AnswerAdminResponse(
                                a.getAnswerId(),
                                a.getAnswerText(),
                                a.getIsCorrect(),  // hiển thị đáp án đúng
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
                partResponses
        );
    }







}
