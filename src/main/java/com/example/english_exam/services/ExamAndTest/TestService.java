package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.*;
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
import com.example.english_exam.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final AnswerRepository answerRepository;
    private final AuthService  authService;
    private final UserTestService userTestService;



    @Transactional
    public TestResponse createTestFromQuestionBank(TestRequest request,
                                                   MultipartFile bannerFile,
                                                   HttpServletRequest httpRequest) throws IOException {
        // === 1. Tạo Test chính ===

        Long currentUserId = authService.getCurrentUserId(httpRequest);

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(currentUserId);
        test.setDurationMinutes(request.getDurationMinutes());
        test.setCreatedAt(LocalDateTime.now());
        test.setAvailableFrom(parseDate(request.getAvailableFrom()));
        test.setAvailableTo(parseDate(request.getAvailableTo()));
        test.setMaxAttempts(request.getMaxAttempts());

        // 🔹 Gắn classId nếu có (có thể null)
        if (request.getClassId() != null) {
            test.setClassId(request.getClassId());
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        testRepository.save(test);

        // === 2. Tạo các phần của bài thi ===
        for (PartRequest partReq : request.getParts()) {
            if (partReq.getExamPartId() == null) continue;

            // 🧩 Tạo TestPart mới
            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());

            // ✅ numQuestions không null
            int numQs = 0;
            if (Boolean.TRUE.equals(partReq.isRandom())) {
                numQs = partReq.getNumQuestions() != null ? partReq.getNumQuestions() : 0;
            } else if (partReq.getQuestionIds() != null) {
                numQs = partReq.getQuestionIds().size();
            }
            testPart.setNumQuestions(numQs);
            testPartRepository.save(testPart);

            // === 3. Random hoặc chọn thủ công câu hỏi ===
            if (partReq.isRandom()) {
                if (numQs <= 0) continue;

                // 🧠 Random 1 câu để kiểm tra xem có passage không
                Question anyQ = questionRepository.findOneRandomQuestion(partReq.getExamPartId());
                if (anyQ == null) continue;

                if (anyQ.getPassageId() != null) {
                    // Nếu có passage → lấy toàn bộ câu hỏi thuộc passage đó
                    List<Question> group = questionRepository.findByPassageId(anyQ.getPassageId());
                    for (Question q : group) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                } else {
                    // Nếu không có passage → random độc lập
                    List<Question> randomQuestions = questionRepository.findRandomQuestionsByExamPartId(
                            partReq.getExamPartId(),
                            PageRequest.of(0, numQs)
                    );

                    for (Question q : randomQuestions) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                }
            } else {
                // 🔹 Chọn thủ công từ danh sách questionIds
                if (partReq.getQuestionIds() != null && !partReq.getQuestionIds().isEmpty()) {
                    for (Long qid : partReq.getQuestionIds()) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(qid);
                        testQuestionRepository.save(tq);
                    }
                }
            }
        }

        return new TestResponse(test);
    }



    private LocalDateTime parseDate(String input) {
        return (input == null || input.isEmpty()) ? null : LocalDateTime.parse(input);
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

    @Transactional
    public TestResponse getTestFullById(Long testId, HttpServletRequest httpRequest) {

        Long currentUserId = authService.getCurrentUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng. Token không hợp lệ hoặc đã hết hạn.");
        }

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // 🟢 Nếu user có bài đang làm mà đã hết giờ -> tự động chuyển sang COMPLETED
        UserTest latest = userTestRepository.findTopByUserIdAndTestIdOrderByStartedAtDesc(currentUserId, testId)
                .orElse(null);

        if (latest != null && latest.getStatus() == UserTest.Status.IN_PROGRESS) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = latest.getStartedAt().plusMinutes(test.getDurationMinutes());

            if (test.getAvailableTo() != null && test.getAvailableTo().isBefore(endTime)) {
                endTime = test.getAvailableTo();
            }

            // 🕒 Nếu hết giờ thì tự động nộp bài
            if (now.isAfter(endTime) || now.isEqual(endTime)) {
                System.out.println("⏰ Auto-submitting UserTest ID = " + latest.getUserTestId());
                try {
                    // 🟢 Gọi service chấm bài và cập nhật điểm
                    userTestService.submitTest(latest.getUserTestId());
                } catch (Exception e) {
                    System.err.println("⚠️ Auto-submit failed for UserTest " + latest.getUserTestId() + ": " + e.getMessage());
                    // Nếu lỗi thì vẫn đóng bài, tránh kẹt trạng thái
                    latest.setStatus(UserTest.Status.COMPLETED);
                    latest.setFinishedAt(endTime);
                    userTestRepository.save(latest);
                }
            }
        }

        // === 1. Tính lại số lượt ===
        TestStatus currentStatus = test.calculateStatus();
        int attemptsUsed = userTestRepository.countByUserIdAndTestIdAndStatus(
                currentUserId,
                testId,
                UserTest.Status.COMPLETED
        );

        Integer maxAttempts = test.getMaxAttempts();
        Integer remaining = (maxAttempts != null)
                ? Math.max(0, maxAttempts - attemptsUsed)
                : null;

        // === 2. Lấy batch data ===
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());
        if (testParts.isEmpty()) {
            return new TestResponse(
                    test.getTestId(), test.getTitle(), test.getDescription(),
                    test.getExamTypeId(), test.getCreatedBy(), test.getCreatedAt(),
                    test.getBannerUrl(), test.getDurationMinutes(), test.getAvailableFrom(),
                    test.getAvailableTo(), currentStatus.name(), maxAttempts,
                    attemptsUsed, remaining, Collections.emptyList()
            );
        }

        List<Long> testPartIds = testParts.stream()
                .map(TestPart::getTestPartId)
                .toList();

        List<TestQuestion> allTestQuestions = testQuestionRepository.findByTestPartIdIn(testPartIds);
        Map<Long, List<TestQuestion>> questionsByPartId = allTestQuestions.stream()
                .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        List<Long> allQuestionIds = allTestQuestions.stream()
                .map(TestQuestion::getQuestionId)
                .distinct()
                .toList();

        List<Question> questionList = allQuestionIds.isEmpty()
                ? Collections.emptyList()
                : questionRepository.findAllById(allQuestionIds);

        Map<Long, Question> questionMap = questionList.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> allPassageIds = questionMap.values().stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Passage> passageList = allPassageIds.isEmpty()
                ? Collections.emptyList()
                : passageRepository.findAllById(allPassageIds);

        Map<Long, Passage> passageMap = passageList.stream()
                .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        Map<Long, List<AnswerResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestions(allQuestionIds);

        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {
            List<TestQuestion> tqList = questionsByPartId.getOrDefault(tp.getTestPartId(), Collections.emptyList());

            PassageResponse passageResponse = tqList.stream()
                    .map(tq -> questionMap.get(tq.getQuestionId()))
                    .filter(q -> q != null && q.getPassageId() != null)
                    .findFirst()
                    .map(q -> passageMap.get(q.getPassageId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PassageResponse(
                            p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType().name()
                    ))
                    .orElse(null);

            List<QuestionResponse> questionResponses = tqList.stream()
                    .map(tq -> {
                        Question q = questionMap.get(tq.getQuestionId());
                        if (q == null) return null;

                        List<AnswerResponse> answers =
                                answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                        return new QuestionResponse(
                                q.getQuestionId(), q.getExamPartId(), q.getQuestionText(),
                                q.getQuestionType(), q.getExplanation(), tp.getTestPartId(), answers
                        );
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new TestPartResponse(
                    tp.getTestPartId(), tp.getExamPartId(), tp.getNumQuestions(),
                    passageResponse, questionResponses
            );
        }).toList();

        // === 4. Trả kết quả ===
        return new TestResponse(
                test.getTestId(), test.getTitle(), test.getDescription(), test.getExamTypeId(),
                test.getCreatedBy(), test.getCreatedAt(), test.getBannerUrl(), test.getDurationMinutes(),
                test.getAvailableFrom(), test.getAvailableTo(), currentStatus.name(),
                maxAttempts, attemptsUsed, remaining, partResponses
        );
    }


// Giả sử phương thức này nằm trong TestService.java

    public TestAdminResponse getTestFullByIdAdmin(Long testId) {
        // === LẤY DỮ LIỆU CƠ BẢN ===
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // === BƯỚC 1: LẤY DỮ LIỆU HÀNG LOẠT ĐỂ TỐI ƯU HÓA (BATCH FETCHING) ===

        // Lấy tất cả TestPart của Test (1 query)
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());
        if (testParts.isEmpty()) {
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
                    test.calculateStatus().name(),
                    test.getMaxAttempts(),
                    Collections.emptyList(),
                    test.getClassId()   // ✅ thêm dòng này
            );
        }

        List<Long> testPartIds = testParts.stream().map(TestPart::getTestPartId).toList();

        // Lấy tất cả TestQuestion của các TestPart (1 query)
        List<TestQuestion> allTestQuestions = testQuestionRepository.findByTestPartIdIn(testPartIds);
        Map<Long, List<TestQuestion>> questionsByPartId = allTestQuestions.stream()
                .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        // Lấy tất cả Question từ các TestQuestion (1 query)
        List<Long> allQuestionIds = allTestQuestions.stream().map(TestQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(allQuestionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Lấy tất cả Passage liên quan (1 query)
        Set<Long> allPassageIds = questionMap.values().stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Passage> passageMap = passageRepository.findAllById(allPassageIds).stream()
                .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        // Lấy tất cả Answer liên quan cho admin (1 query)
        Map<Long, List<AnswerAdminResponse>> answersByQuestionId = answerService.getAnswersForMultipleQuestionsForAdmin(allQuestionIds);

        // === BƯỚC 2: LẮP RÁP DỮ LIỆU TRONG BỘ NHỚ (NO MORE DATABASE CALLS) ===

        List<TestPartAdminResponse> partResponses = testParts.stream().map(tp -> {

            List<TestQuestion> tqList = questionsByPartId.getOrDefault(tp.getTestPartId(), Collections.emptyList());

            // Lấy passage cho cả part này
            PassageResponse passageResponseForPart = tqList.stream()
                    .map(tq -> questionMap.get(tq.getQuestionId()))
                    .filter(q -> q != null && q.getPassageId() != null)
                    .findFirst()
                    .map(q -> passageMap.get(q.getPassageId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PassageResponse(p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType().name()))
                    .orElse(null);

            List<QuestionAdminResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionMap.get(tq.getQuestionId());
                if (q == null) return null;

                List<AnswerAdminResponse> answers = answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                // Constructor của QuestionAdminResponse giờ đã đơn giản hơn
                return new QuestionAdminResponse(
                        q.getQuestionId(), q.getExamPartId(), q.getQuestionText(),
                        q.getQuestionType(), q.getExplanation(), tp.getTestPartId(),
                        answers,q.getClassId()  // ✅ thêm dòng này

                );
            }).filter(Objects::nonNull).toList();

            // Gán passageResponse vào TestPartAdminResponse
            return new TestPartAdminResponse(
                    tp.getTestPartId(), tp.getExamPartId(), tp.getNumQuestions(), passageResponseForPart, questionResponses
            );
        }).toList();

        // === BƯỚC 3: TRẢ VỀ RESPONSE ===
        return new TestAdminResponse(
                test.getTestId(), test.getTitle(), test.getDescription(), test.getExamTypeId(),
                test.getCreatedBy(), test.getCreatedAt(), test.getBannerUrl(), test.getDurationMinutes(),
                test.getAvailableFrom(), test.getAvailableTo(), test.calculateStatus().name(),
                test.getMaxAttempts(), partResponses,test.getClassId()
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

        int attemptsUsed = userTestRepository.countByUserIdAndTestIdAndStatus(
                userId,
                test.getTestId(),
                UserTest.Status.COMPLETED
        );        Integer maxAttempts = test.getMaxAttempts();

        if (maxAttempts != null && attemptsUsed >= maxAttempts) {
            result.put("canStart", false);
            result.put("message", "Bạn đã hết số lượt làm bài");
            return result;
        }

        result.put("canStart", true);
        result.put("message", "OK");
        return result;
    }


    @Transactional
    public TestResponse createTestWithNewQuestions(CreateTestWithQuestionsRequest request,
                                                   MultipartFile bannerFile,
                                                   List<MultipartFile> audioFiles,
                                                   HttpServletRequest httpRequest) throws IOException {


        Long currentUserId = authService.getCurrentUserId(httpRequest);

        // === BƯỚC 1: TẠO TEST CHÍNH ===
        ExamType examType = examTypeRepository.findById(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("ExamType not found with id: " + request.getExamTypeId()));

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(currentUserId); // ✅ Gán theo user đăng nhập thật
        test.setCreatedAt(LocalDateTime.now());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setAvailableFrom(request.getAvailableFrom());
        test.setAvailableTo(request.getAvailableTo());
        test.setMaxAttempts(request.getMaxAttempts());

        // 🔹 Gắn classId nếu có (có thể null)
        if (request.getClassId() != null) {
            test.setClassId(request.getClassId());
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        test = testRepository.save(test);

        List<TestPartResponse> partResponses = new ArrayList<>();
        int audioIndex = 0; // Đếm audio cho từng passage LISTENING

        // === BƯỚC 2: LẶP QUA PARTS ===
        for (PartWithQuestionsRequest partReq : request.getParts()) {
            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getQuestions().size());
            testPart = testPartRepository.save(testPart);

            Long passageId = null;
            PassageResponse passageResponse = null;

            // === TẠO PASSAGE ===
            if (partReq.getPassage() != null) {
                Passage newPassage = new Passage();
                newPassage.setContent(partReq.getPassage().getContent());
                newPassage.setPassageType(partReq.getPassage().getPassageType());

                // === Nếu là LISTENING thì lấy file audio tương ứng ===
                if (newPassage.getPassageType() == Passage.PassageType.LISTENING) {
                    if (audioFiles != null && audioIndex < audioFiles.size()) {
                        MultipartFile audioFile = audioFiles.get(audioIndex);
                        if (audioFile != null && !audioFile.isEmpty()) {
                            // 🟢 Upload audio lên Cloudinary
                            String audioUrl = cloudinaryService.uploadAudio(audioFile);
                            newPassage.setMediaUrl(audioUrl);
                            System.out.println("✅ Uploaded audio for passage: " + newPassage.getContent());
                        } else {
                            System.out.println("⚠️ Audio file " + audioIndex + " is empty or null");
                        }
                    } else {
                        System.out.println("⚠️ No audio file provided for passage index " + audioIndex);
                    }
                    audioIndex++; // chỉ tăng khi passage là LISTENING
                } else {
                    newPassage.setMediaUrl(partReq.getPassage().getMediaUrl());
                }

                Passage savedPassage = passageRepository.save(newPassage);
                passageId = savedPassage.getPassageId();

                passageResponse = new PassageResponse(
                        savedPassage.getPassageId(),
                        savedPassage.getContent(),
                        savedPassage.getMediaUrl(),
                        savedPassage.getPassageType().name()
                );
            }

            // === LẶP QUA CÂU HỎI ===
            List<QuestionResponse> questionResponses = new ArrayList<>();

            for (NormalQuestionRequest questionReq : partReq.getQuestions()) {
                Question newQuestion = new Question();
                newQuestion.setExamPartId(testPart.getExamPartId());
                newQuestion.setPassageId(passageId);
                newQuestion.setQuestionText(questionReq.getQuestionText());
                newQuestion.setQuestionType(questionReq.getQuestionType());
                newQuestion.setCreatedBy(currentUserId);
                newQuestion = questionRepository.save(newQuestion);

                List<Answer> newAnswers = new ArrayList<>();
                if (questionReq.getAnswers() != null && !questionReq.getAnswers().isEmpty()) {
                    List<Answer> answersToSave = new ArrayList<>();
                    for (AnswerRequest ar : questionReq.getAnswers()) {
                        Answer ans = new Answer();
                        ans.setQuestionId(newQuestion.getQuestionId());
                        ans.setAnswerText(ar.getAnswerText());
                        ans.setAnswerLabel(ar.getLabel() != null ? ar.getLabel() : "");
                        ans.setIsCorrect(ar.getIsCorrect() != null && ar.getIsCorrect());
                        answersToSave.add(ans);
                    }
                    newAnswers = answerRepository.saveAll(answersToSave);
                }

                TestQuestion link = new TestQuestion();
                link.setTestPartId(testPart.getTestPartId());
                link.setQuestionId(newQuestion.getQuestionId());
                testQuestionRepository.save(link);

                List<AnswerResponse> answerResponses = newAnswers.stream()
                        .map(ans -> new AnswerResponse(ans.getAnswerId(), ans.getAnswerText(), ans.getAnswerLabel()))
                        .collect(Collectors.toList());

                questionResponses.add(new QuestionResponse(
                        newQuestion.getQuestionId(),
                        testPart.getExamPartId(),
                        newQuestion.getQuestionText(),
                        newQuestion.getQuestionType(),
                        null,
                        testPart.getTestPartId(),
                        answerResponses
                ));
            }

            // === Build TestPartResponse ===
            partResponses.add(new TestPartResponse(
                    testPart.getTestPartId(),
                    testPart.getExamPartId(),
                    testPart.getNumQuestions(),
                    passageResponse,
                    questionResponses
            ));
        }

        // === BƯỚC 3: TRẢ VỀ RESPONSE ===
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
                test.calculateStatus().name(),
                test.getMaxAttempts(),
                0,
                test.getMaxAttempts(),
                partResponses
        );
    }

}