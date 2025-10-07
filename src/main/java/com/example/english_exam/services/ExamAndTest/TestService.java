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
import com.example.english_exam.security.JwtService;
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
    private final JwtService jwtService;



    @Transactional
    public TestResponse createTestFromQuestionBank(CreateTestRequest request, MultipartFile bannerFile) throws IOException {
        // === 1. Tạo Test chính ===
        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(request.getCreateBy());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setCreatedAt(LocalDateTime.now());
        test.setAvailableFrom(parseDate(request.getAvailableFrom()));
        test.setAvailableTo(parseDate(request.getAvailableTo()));
        test.setMaxAttempts(request.getMaxAttempts());

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

    @Transactional
    public TestResponse createTest(TestRequest request, MultipartFile bannerFile) throws IOException {
        // === PHẦN TẠO TEST CHÍNH ===
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

        test = testRepository.save(test);

        List<TestPartResponse> partResponses = new ArrayList<>();

        // === LẶP QUA CÁC PART ===
        for (PartRequest partReq : request.getParts()) {
            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getNumQuestions());
            testPart = testPartRepository.save(testPart);

            final Long testPartId = testPart.getTestPartId();


            // Lấy các câu hỏi ngẫu nhiên cho part này (1 query)
            List<Question> questions = questionRepository.findRandomByExamPart(
                    partReq.getExamPartId(), partReq.getNumQuestions());

            if (questions.isEmpty()) {
                partResponses.add(new TestPartResponse(testPart.getTestPartId(), testPart.getExamPartId(), 0, null, Collections.emptyList()));
                continue;
            }

            // --- BƯỚC 1: TỐI ƯU HÓA - LẤY DỮ LIỆU HÀNG LOẠT ---
            List<Long> questionIds = questions.stream().map(Question::getQuestionId).toList();

            List<TestQuestion> testQuestionsToSave = new ArrayList<>();
            for (Long qId : questionIds) {
                TestQuestion tq = new TestQuestion();
                tq.setTestPartId(testPart.getTestPartId());
                tq.setQuestionId(qId);
                testQuestionsToSave.add(tq);
            }
            testQuestionRepository.saveAll(testQuestionsToSave);

            Set<Long> passageIds = questions.stream().map(Question::getPassageId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, Passage> passageMap = passageRepository.findAllById(passageIds).stream()
                    .collect(Collectors.toMap(Passage::getPassageId, p -> p));

            Map<Long, List<AnswerResponse>> answersByQuestionId = answerService.getAnswersForMultipleQuestions(questionIds);

            // --- BƯỚC 2: LẮP RÁP DỮ LIỆU ĐÃ LẤY ---

            PassageResponse passageResponseForPart = questions.stream()
                    .map(Question::getPassageId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(passageMap::get)
                    .map(p -> new PassageResponse(p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType().name()))
                    .orElse(null);

            List<QuestionResponse> questionResponses = questions.stream().map(q -> {
                List<AnswerResponse> answers = answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                return new QuestionResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        testPartId,  // ✅ dùng biến final thay vì testPart
                        answers
                );
            }).toList();


            partResponses.add(new TestPartResponse(
                    testPart.getTestPartId(),
                    testPart.getExamPartId(),
                    testPart.getNumQuestions(),
                    passageResponseForPart,
                    questionResponses
            ));
        }

        // --- DÒNG RETURN HOÀN CHỈNH ---
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
                0, // Mới tạo nên attemptsUsed = 0
                test.getMaxAttempts(), // remainingAttempts = maxAttempts
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
        // === LẤY DỮ LIỆU CƠ BẢN ===
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        TestStatus currentStatus = test.calculateStatus();
        int attemptsUsed = userTestRepository.countByUserIdAndTestId(userId, testId);
        Integer maxAttempts = test.getMaxAttempts();
        Integer remaining = (maxAttempts != null) ? Math.max(0, maxAttempts - attemptsUsed) : null;

        // === BƯỚC 1: LẤY DỮ LIỆU HÀNG LOẠT ĐỂ TỐI ƯU HÓA (BATCH FETCHING) ===

        // Lấy tất cả TestPart của Test (1 query)
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());
        if (testParts.isEmpty()) {
            // Trả về sớm nếu không có phần nào
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
                    Collections.emptyList()
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

        // Lấy tất cả Answer liên quan (1 query)
        Map<Long, List<AnswerResponse>> answersByQuestionId = answerService.getAnswersForMultipleQuestions(allQuestionIds);

        // === BƯỚC 2: LẮP RÁP DỮ LIỆU TRONG BỘ NHỚ (NO MORE DATABASE CALLS) ===

        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {

            List<TestQuestion> tqList = questionsByPartId.getOrDefault(tp.getTestPartId(), Collections.emptyList());

            // Lấy passage cho cả part này
            PassageResponse passageResponseForPart = tqList.stream()
                    .map(tq -> questionMap.get(tq.getQuestionId()))
                    .filter(q -> q != null && q.getPassageId() != null)
                    .findFirst() // Chỉ cần tìm thấy passage đầu tiên là đủ
                    .map(q -> passageMap.get(q.getPassageId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PassageResponse(p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType().name()))
                    .orElse(null); // Sẽ là null nếu không có câu hỏi nào có passage

            List<QuestionResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionMap.get(tq.getQuestionId());
                if (q == null) return null;

                List<AnswerResponse> answers = answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                // Constructor của QuestionResponse giờ đã đơn giản hơn
                return new QuestionResponse(
                        q.getQuestionId(), q.getExamPartId(), q.getQuestionText(),
                        q.getQuestionType(), q.getExplanation(), tp.getTestPartId(),
                        answers
                );
            }).filter(Objects::nonNull).toList();

            // Gán passageResponse vào TestPartResponse
            return new TestPartResponse(
                    tp.getTestPartId(), tp.getExamPartId(), tp.getNumQuestions(), passageResponseForPart, questionResponses
            );
        }).toList();

        // === BƯỚC 3: TRẢ VỀ RESPONSE ===
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
                    Collections.emptyList()
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
                        answers
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
                test.getMaxAttempts(), partResponses
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


    @Transactional
    public TestResponse createTestWithNewQuestions(CreateTestWithQuestionsRequest request,
                                                   MultipartFile bannerFile,
                                                   List<MultipartFile> audioFiles) throws IOException {

        // === BƯỚC 1: TẠO TEST CHÍNH ===
        ExamType examType = examTypeRepository.findById(request.getExamTypeId())
                .orElseThrow(() -> new RuntimeException("ExamType not found with id: " + request.getExamTypeId()));

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(request.getCreateBy());
        test.setCreatedAt(LocalDateTime.now());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setAvailableFrom(request.getAvailableFrom());
        test.setAvailableTo(request.getAvailableTo());
        test.setMaxAttempts(request.getMaxAttempts());

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