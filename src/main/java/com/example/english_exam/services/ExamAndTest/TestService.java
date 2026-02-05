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
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private final ExamPartRepository  examPartRepository;
    private final CloudinaryService  cloudinaryService;
    private final ExamTypeRepository examTypeRepository;
    private final PassageRepository  passageRepository;
    private final UserTestRepository userTestRepository;
    private final AnswerRepository answerRepository;
    private final AuthUtils authUtils;
    private final UserTestService userTestService;
    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ChapterRepository chapterRepository;
    private final QuestionService questionService;


    private Test createEmptyTest(CreateTestWithQuestionsRequest request,
                                 MultipartFile bannerFile,
                                 Long currentUserId) throws IOException {

        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(currentUserId);
        test.setCreatedAt(LocalDateTime.now());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setAvailableFrom(request.getAvailableFrom());
        test.setAvailableTo(request.getAvailableTo());
        test.setMaxAttempts(request.getMaxAttempts());

        if (request.getClassId() != null) {
            test.setClassId(request.getClassId());
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        return testRepository.save(test);
    }

    @Transactional
    public TestResponse createTestFromQuestionBank(TestRequest request,
                                                   MultipartFile bannerFile,
                                                   HttpServletRequest httpRequest) throws IOException {

        // === 1️⃣ Lấy thông tin người tạo ===
        Long currentUserId = authUtils.getUserId(httpRequest);

        // === 2️⃣ Tạo Test chính ===
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
        Long classId = request.getClassId();
        if (classId != null) {
            test.setClassId(classId);
        }

        // 🔹 Upload banner nếu có
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            test.setBannerUrl(url);
        }

        testRepository.save(test);

        // === 3️⃣ Tạo các phần (Part) của bài thi ===
        for (PartRequest partReq : request.getParts()) {
            if (partReq.getExamPartId() == null) continue;

            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());

            // ✅ Tính số lượng câu hỏi
            int numQs = 0;
            if (Boolean.TRUE.equals(partReq.isRandom())) {
                numQs = partReq.getNumQuestions() != null ? partReq.getNumQuestions() : 0;
            } else if (partReq.getQuestionIds() != null) {
                numQs = partReq.getQuestionIds().size();
            }
            testPart.setNumQuestions(numQs);
            testPartRepository.save(testPart);

            // === 4️⃣ Random hoặc chọn thủ công câu hỏi ===
            if (partReq.isRandom()) {
                if (numQs <= 0) continue;

                // 🧠 Random 1 câu để kiểm tra có passage không
                Question anyQ = (classId != null)
                        ? questionRepository.findOneRandomQuestionByClass(partReq.getExamPartId(), classId)
                        : questionRepository.findOneRandomQuestion(partReq.getExamPartId());
                if (anyQ == null) continue;

                if (anyQ.getPassageId() != null) {
                    // 🔹 Lấy toàn bộ câu hỏi cùng passage, vẫn lọc theo classId của câu hỏi
                    List<Question> group = (classId != null)
                            ? questionRepository.findByPassageIdAndClassId(anyQ.getPassageId(), classId)
                            : questionRepository.findByPassageId(anyQ.getPassageId());

                    for (Question q : group) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                } else {
                    // 🔹 Random độc lập
                    List<Question> randomQuestions = (classId != null)
                            ? questionRepository.findRandomQuestionsByExamPartIdAndClassId(
                            partReq.getExamPartId(), classId, PageRequest.of(0, numQs))
                            : questionRepository.findRandomQuestionsByExamPartId(
                            partReq.getExamPartId(), PageRequest.of(0, numQs));

                    for (Question q : randomQuestions) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                }

            } else {
                // 🔹 Chọn thủ công
                if (partReq.getQuestionIds() != null && !partReq.getQuestionIds().isEmpty()) {
                    for (Long qid : partReq.getQuestionIds()) {
                        if (classId != null) {
                            Question q = questionRepository.findById(qid)
                                    .orElseThrow(() -> new RuntimeException("Question not found"));
                            if (!classId.equals(q.getClassId())) continue; // bỏ qua câu hỏi khác lớp
                        }

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

        // === 0️⃣ Xác định người dùng hiện tại từ token ===
        // Nếu không có userId → token hết hạn hoặc không hợp lệ
        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng. Token không hợp lệ hoặc đã hết hạn.");
        }

        // === 1️⃣ Lấy thông tin bài test theo ID ===
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // === 2️⃣ Kiểm tra nếu user có bài đang làm dở → auto submit nếu đã hết giờ ===
        UserTest latest = userTestRepository.findTopByUserIdAndTestIdOrderByStartedAtDesc(currentUserId, testId)
                .orElse(null);

        if (latest != null && latest.getStatus() == UserTest.Status.IN_PROGRESS) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = latest.getStartedAt().plusMinutes(test.getDurationMinutes());

            // Nếu bài thi có thời gian đóng cửa (availableTo) sớm hơn thời gian hết bài → cắt về thời điểm đó
            if (test.getAvailableTo() != null && test.getAvailableTo().isBefore(endTime)) {
                endTime = test.getAvailableTo();
            }

            // Nếu hiện tại >= thời điểm kết thúc → tự động nộp bài
            if (now.isAfter(endTime) || now.isEqual(endTime)) {
                System.out.println("⏰ Auto-submitting UserTest ID = " + latest.getUserTestId());
                try {
                    userTestService.submitTest(latest.getUserTestId());
                } catch (Exception e) {
                    System.err.println("⚠️ Auto-submit failed for UserTest " + latest.getUserTestId() + ": " + e.getMessage());
                    // Nếu auto-submit lỗi → đánh dấu hoàn thành thủ công
                    latest.setStatus(UserTest.Status.COMPLETED);
                    latest.setFinishedAt(endTime);
                    userTestRepository.save(latest);
                }
            }
        }

        // === 3️⃣ Tính số lượt làm còn lại cho người dùng hiện tại ===
        TestStatus currentStatus = test.calculateStatus();

        // Đếm số lần người dùng đã hoàn thành bài thi này
        int attemptsUsed = userTestRepository.countByUserIdAndTestIdAndStatus(
                currentUserId, testId, UserTest.Status.COMPLETED
        );

        Integer maxAttempts = test.getMaxAttempts();
        Integer remaining = (maxAttempts != null)
                ? Math.max(0, maxAttempts - attemptsUsed)
                : null; // null nghĩa là không giới hạn lượt làm

        // === 🔒 Nếu người dùng đã hết lượt thi, trả về response đặc biệt (không ném lỗi nữa) ===
        if (maxAttempts != null && remaining <= 0) {
            TestResponse blocked = new TestResponse(test);
            blocked.setCanDoTest(false);  // 🚫 Không được làm bài nữa
            blocked.setAttemptsUsed(attemptsUsed);
            blocked.setRemainingAttempts(remaining);
            blocked.setStatus("FORBIDDEN"); // hiển thị rõ ràng trạng thái bị chặn
            return blocked;
        }

        // === 4️⃣ Lấy danh sách các phần thi (parts) của bài test ===
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());
        if (testParts.isEmpty()) {
            // Nếu test không có part → trả về rỗng (tránh lỗi null)
            TestResponse res = new TestResponse();
            res.setTestId(test.getTestId());
            res.setTitle(test.getTitle());
            res.setDescription(test.getDescription());
            res.setExamTypeId(test.getExamTypeId());
            res.setCreatedBy(test.getCreatedBy());
            res.setCreatedAt(test.getCreatedAt());
            res.setBannerUrl(test.getBannerUrl());
            res.setDurationMinutes(test.getDurationMinutes());
            res.setAvailableFrom(test.getAvailableFrom());
            res.setAvailableTo(test.getAvailableTo());
            res.setStatus(currentStatus.name());
            res.setMaxAttempts(maxAttempts);
            res.setAttemptsUsed(attemptsUsed);
            res.setRemainingAttempts(remaining);
            res.setParts(Collections.emptyList());
            res.setCanDoTest(true); // ✅ Vẫn cho phép làm bài
            return res;
        }

        // === 5️⃣ Chuẩn bị dữ liệu câu hỏi & passage ===
        List<Long> testPartIds = testParts.stream().map(TestPart::getTestPartId).toList();

        // Lấy toàn bộ câu hỏi của các part
        List<TestQuestion> allTestQuestions = testQuestionRepository.findByTestPartIdIn(testPartIds);

        // Gom câu hỏi theo testPartId
        Map<Long, List<TestQuestion>> questionsByPartId =
                allTestQuestions.stream().collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        // Lấy danh sách questionId thực tế
        List<Long> allQuestionIds = allTestQuestions.stream()
                .map(TestQuestion::getQuestionId)
                .distinct()
                .toList();

        // Load chi tiết Question trong bảng question
        List<Question> questionList = allQuestionIds.isEmpty()
                ? Collections.emptyList()
                : questionRepository.findAllById(allQuestionIds);

        // Map questionId → Question
        Map<Long, Question> questionMap = questionList.stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Lấy toàn bộ passage liên quan (đoạn đọc / nghe)
        Set<Long> allPassageIds = questionMap.values().stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Passage> passageList = allPassageIds.isEmpty()
                ? Collections.emptyList()
                : passageRepository.findAllById(allPassageIds);

        Map<Long, Passage> passageMap = passageList.stream()
                .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        // Lấy danh sách đáp án cho nhiều câu hỏi cùng lúc
        Map<Long, List<AnswerResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestions(allQuestionIds);

        // === 6️⃣ Xử lý từng phần thi (Part) ===
        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {

            // 🟢 Lấy tất cả câu hỏi thuộc part này
            List<TestQuestion> tqList = new ArrayList<>(
                    questionsByPartId.getOrDefault(tp.getTestPartId(), Collections.emptyList())
            );

            // 🧩 Gom nhóm theo passage: cùng passageId thì cùng nhóm
            // Nếu câu hỏi không có passage → nhóm vào -1 (câu lẻ)
            Map<Long, List<TestQuestion>> groupedByPassage = tqList.stream()
                    .collect(Collectors.groupingBy(tq -> {
                        Question q = questionMap.get(tq.getQuestionId());
                        return (q != null && q.getPassageId() != null) ? q.getPassageId() : -1L;
                    }));

            // Danh sách nhóm (mỗi nhóm là 1 passage hoặc 1 câu lẻ)
            List<List<TestQuestion>> questionGroups = new ArrayList<>(groupedByPassage.values());

            // 🌀 Random thứ tự passage
            Collections.shuffle(questionGroups);
            // 🔀 Random thứ tự câu hỏi trong từng passage
            for (List<TestQuestion> group : questionGroups) {
                Collections.shuffle(group);
            }

            // ✅ Gộp lại danh sách câu hỏi sau khi random
            List<TestQuestion> randomizedTqList = questionGroups.stream()
                    .flatMap(List::stream)
                    .toList();

            // === 7️⃣ Tạo PassageResponse (nếu có passage thật) ===
            PassageResponse passageResponse = randomizedTqList.stream()
                    .map(tq -> questionMap.get(tq.getQuestionId()))
                    .filter(q -> q != null && q.getPassageId() != null)
                    .findFirst()
                    .map(q -> passageMap.get(q.getPassageId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PassageResponse(
                            p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType()
                    ))
                    .orElse(null);

            // === 8️⃣ Chuyển đổi danh sách câu hỏi + đáp án sang DTO QuestionResponse ===
            List<QuestionResponse> questionResponses = randomizedTqList.stream()
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

            // === 9️⃣ Tạo DTO cho từng phần thi ===
            return new TestPartResponse(
                    tp.getTestPartId(), tp.getExamPartId(), tp.getNumQuestions(),
                    passageResponse, questionResponses
            );
        }).toList();

        // === 🔟 Trả về TestResponse hoàn chỉnh cho FE ===
        return new TestResponse(
                test.getTestId(), test.getTitle(), test.getDescription(), test.getExamTypeId(),
                test.getCreatedBy(), test.getCreatedAt(), test.getBannerUrl(), test.getDurationMinutes(),
                test.getAvailableFrom(), test.getAvailableTo(), currentStatus.name(),
                maxAttempts, attemptsUsed, remaining,
                true, // ✅ canDoTest = true (người dùng còn quyền làm bài)
                partResponses
        );
    }


    public TestAdminResponse getTestFullByIdAdmin(Long testId) {
        // === LẤY DỮ LIỆU CƠ BẢN ===
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // === BƯỚC 1: LẤY DỮ LIỆU HÀNG LOẠT ĐỂ TỐI ƯU HÓA ===
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
                    test.getClassId()
            );
        }

        List<Long> testPartIds = testParts.stream().map(TestPart::getTestPartId).toList();

        // Lấy tất cả TestQuestion của các TestPart
        List<TestQuestion> allTestQuestions = testQuestionRepository.findByTestPartIdIn(testPartIds);
        Map<Long, List<TestQuestion>> questionsByPartId = allTestQuestions.stream()
                .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        // Lấy tất cả Question
        List<Long> allQuestionIds = allTestQuestions.stream().map(TestQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(allQuestionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        // Lấy tất cả Passage
        Set<Long> allPassageIds = questionMap.values().stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Passage> passageMap = passageRepository.findAllById(allPassageIds).stream()
                .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        // Lấy tất cả Answer
        Map<Long, List<AnswerAdminResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestionsForAdmin(allQuestionIds);

        // === BƯỚC 2: LẮP RÁP DỮ LIỆU TRONG BỘ NHỚ ===
        List<TestPartAdminResponse> partResponses = testParts.stream().map(tp -> {
            List<TestQuestion> tqList = questionsByPartId.getOrDefault(tp.getTestPartId(), Collections.emptyList());

            // Lấy passage chung cho part này (nếu có)
            PassageResponse passageResponseForPart = tqList.stream()
                    .map(tq -> questionMap.get(tq.getQuestionId()))
                    .filter(q -> q != null && q.getPassageId() != null)
                    .findFirst()
                    .map(q -> passageMap.get(q.getPassageId()))
                    .filter(Objects::nonNull)
                    .map(p -> new PassageResponse(
                            p.getPassageId(),
                            p.getContent(),
                            p.getMediaUrl(),
                            p.getPassageType()
                    ))
                    .orElse(null);

            // Lắp danh sách câu hỏi
            List<QuestionAdminResponse> questionResponses = tqList.stream().map(tq -> {
                Question q = questionMap.get(tq.getQuestionId());
                if (q == null) return null;

                List<AnswerAdminResponse> answers =
                        answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                PassageResponse passageDto = null;
                if (q.getPassageId() != null) {
                    Passage p = passageMap.get(q.getPassageId());
                    if (p != null) {
                        passageDto = new PassageResponse(
                                p.getPassageId(),
                                p.getContent(),
                                p.getMediaUrl(),
                                p.getPassageType()
                        );
                    }
                }

                // Lấy examTypeId từ examPart
                Long examTypeId = examPartRepository.findById(q.getExamPartId())
                        .map(ExamPart::getExamTypeId)
                        .orElse(null);

                return new QuestionAdminResponse(
                        q.getQuestionId(),
                        examTypeId,                 // 🟢 thêm mới
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        passageDto,                 // 🟢 thêm mới
                        tp.getTestPartId(),
                        answers,
                        q.getClassId()
                );
            }).filter(Objects::nonNull).toList();

            // Tạo TestPartAdminResponse
            return new TestPartAdminResponse(
                    tp.getTestPartId(),
                    tp.getExamPartId(),
                    tp.getNumQuestions(),
                    passageResponseForPart,
                    questionResponses
            );
        }).toList();

        // === BƯỚC 3: TRẢ VỀ RESPONSE CHO ADMIN ===
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
                partResponses,
                test.getClassId()
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

    public List<Test> getTestByClassId(Long classId, HttpServletRequest request) {
        // 🧩 Lấy user hiện tại từ token
        Long currentUserId = authUtils.getUserId(request);
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "🔒 Bạn cần đăng nhập để xem bài kiểm tra.");
        }

        // 🧩 Kiểm tra quyền truy cập lớp
        boolean isMember = classMemberRepository.existsByClassIdAndUserId(classId, currentUserId);
        boolean isTeacher = classRepository.existsByClassIdAndTeacherId(classId, currentUserId);

        if (!isMember && !isTeacher) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "❌ Bạn không có quyền xem bài kiểm tra của lớp này!");
        }

        // ✅ Nếu hợp lệ, trả danh sách bài kiểm tra
        return testRepository.findByClassId(classId);
    }


    public List<Test> getTestByCreateBy(HttpServletRequest request) {
        // 🧩 Lấy user hiện tại từ token
        Long currentUserId = authUtils.getUserId(request);
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "🔒 Bạn cần đăng nhập để xem bài kiểm tra.");
        }

        // ✅ Nếu hợp lệ, trả danh sách bài kiểm tra
        return testRepository.findByCreatedBy(currentUserId);
    }

    @Transactional
    public TestResponse createTestWithNewQuestions(
            CreateTestWithQuestionsRequest request,
            MultipartFile bannerFile,
            List<MultipartFile> audioFiles,
            HttpServletRequest httpRequest) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);

        // ✅ 1. Tạo test
        Test test = createEmptyTest(request, bannerFile, currentUserId);

        List<TestPartResponse> partResponses = new ArrayList<>();

        // ✅ 2. Lặp PART
        for (PartWithQuestionsRequest partReq : request.getParts()) {

            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getQuestions().size());
            testPart = testPartRepository.save(testPart);

            List<QuestionResponse> questionResponses = new ArrayList<>();

            // ✅ 3. GỌI SERVICE TẠO QUESTION
            for (NormalQuestionRequest qReq : partReq.getQuestions()) {

                QuestionRequest qr = new QuestionRequest();
                qr.setExamPartId(partReq.getExamPartId());
                qr.setQuestionText(qReq.getQuestionText());
                qr.setQuestionType(qReq.getQuestionType());
                qr.setAnswers(qReq.getAnswers());
                qr.setClassId(request.getClassId());
                qr.setTestPartId(testPart.getTestPartId());

                QuestionAdminResponse created =
                        questionService.createQuestionWithAnswersAdmin(qr, httpRequest);

                // link đã nằm trong service kia rồi
            }

            partResponses.add(new TestPartResponse(
                    testPart.getTestPartId(),
                    testPart.getExamPartId(),
                    testPart.getNumQuestions(),
                    null,
                    questionResponses
            ));
        }

        return new TestResponse(test);
    }



    @Transactional
    public TestResponse updateTestFromQuestionBank(Long testId,
                                                   TestRequest request,
                                                   MultipartFile bannerFile,
                                                   HttpServletRequest httpRequest) throws IOException {

        // === 1️⃣ Lấy Test hiện có ===
        Test existing = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with ID: " + testId));

        Long currentUserId = authUtils.getUserId(httpRequest);
        Long classId = request.getClassId();

        // 🧩 Kiểm tra quyền sửa (chỉ người tạo hoặc admin)
        if (!existing.getCreatedBy().equals(currentUserId)) {
            throw new RuntimeException("❌ Bạn không có quyền sửa đề thi này!");
        }

        // === 2️⃣ Cập nhật thông tin chung ===
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setExamTypeId(request.getExamTypeId());
        existing.setDurationMinutes(request.getDurationMinutes());
        existing.setAvailableFrom(parseDate(request.getAvailableFrom()));
        existing.setAvailableTo(parseDate(request.getAvailableTo()));
        existing.setMaxAttempts(request.getMaxAttempts());
        existing.setClassId(classId);

        // 🖼️ Cập nhật banner nếu có file mới
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadImage(bannerFile);
            existing.setBannerUrl(url);
        }

        testRepository.save(existing);

        // === 3️⃣ Xóa phần và câu hỏi cũ ===
        List<TestPart> oldParts = testPartRepository.findByTestId(testId);
        for (TestPart tp : oldParts) {
            testQuestionRepository.deleteByTestPartId(tp.getTestPartId());
        }
        testPartRepository.deleteAll(oldParts);

        // === 4️⃣ Tạo lại các phần mới ===
        for (PartRequest partReq : request.getParts()) {
            if (partReq.getExamPartId() == null) continue;

            TestPart testPart = new TestPart();
            testPart.setTestId(existing.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());

            int numQs = 0;
            if (Boolean.TRUE.equals(partReq.isRandom())) {
                numQs = partReq.getNumQuestions() != null ? partReq.getNumQuestions() : 0;
            } else if (partReq.getQuestionIds() != null) {
                numQs = partReq.getQuestionIds().size();
            }
            testPart.setNumQuestions(numQs);
            testPartRepository.save(testPart);

            // === 5️⃣ Random hoặc chọn thủ công ===
            if (partReq.isRandom()) {
                if (numQs <= 0) continue;

                // 🧠 Random 1 câu để xác định passage
                Question anyQ = (classId != null)
                        ? questionRepository.findOneRandomQuestionByClass(partReq.getExamPartId(), classId)
                        : questionRepository.findOneRandomQuestion(partReq.getExamPartId());
                if (anyQ == null) continue;

                if (anyQ.getPassageId() != null) {
                    // 🔹 Lấy các câu cùng passage, vẫn lọc theo classId
                    List<Question> group = (classId != null)
                            ? questionRepository.findByPassageIdAndClassId(anyQ.getPassageId(), classId)
                            : questionRepository.findByPassageId(anyQ.getPassageId());

                    for (Question q : group) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                } else {
                    // 🔹 Random độc lập
                    List<Question> randomQuestions = (classId != null)
                            ? questionRepository.findRandomQuestionsByExamPartIdAndClassId(
                            partReq.getExamPartId(), classId, PageRequest.of(0, numQs))
                            : questionRepository.findRandomQuestionsByExamPartId(
                            partReq.getExamPartId(), PageRequest.of(0, numQs));

                    for (Question q : randomQuestions) {
                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(q.getQuestionId());
                        testQuestionRepository.save(tq);
                    }
                }

            } else {
                // 🔹 Thủ công
                if (partReq.getQuestionIds() != null && !partReq.getQuestionIds().isEmpty()) {
                    for (Long qid : partReq.getQuestionIds()) {
                        if (classId != null) {
                            Question q = questionRepository.findById(qid)
                                    .orElseThrow(() -> new RuntimeException("Question not found"));
                            if (!classId.equals(q.getClassId())) continue;
                        }

                        TestQuestion tq = new TestQuestion();
                        tq.setTestPartId(testPart.getTestPartId());
                        tq.setQuestionId(qid);
                        testQuestionRepository.save(tq);
                    }
                }
            }
        }

        return new TestResponse(existing);
    }

    public TestAdminResponse getTestDetailForAdmin(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        TestAdminResponse dto = new TestAdminResponse();
        dto.setTestId(test.getTestId());
        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setExamTypeId(test.getExamTypeId());
        dto.setCreatedBy(test.getCreatedBy());
        dto.setCreatedAt(test.getCreatedAt());
        dto.setBannerUrl(test.getBannerUrl());
        dto.setDurationMinutes(test.getDurationMinutes());
        dto.setAvailableFrom(test.getAvailableFrom());
        dto.setAvailableTo(test.getAvailableTo());
        dto.setMaxAttempts(test.getMaxAttempts());
        dto.setClassId(test.getClassId());

        // 🔹 Load test parts
        List<TestPart> parts = testPartRepository.findByTestId(testId);
        List<TestPartAdminResponse> partResponses = new ArrayList<>();

        for (TestPart part : parts) {
            TestPartAdminResponse partDto = new TestPartAdminResponse();
            partDto.setTestPartId(part.getTestPartId());
            partDto.setExamPartId(part.getExamPartId());
            partDto.setNumQuestions(part.getNumQuestions());

            // 🔹 Load questions
            List<TestQuestion> tqs = testQuestionRepository.findByTestPartId(part.getTestPartId());
            List<QuestionAdminResponse> questionDtos = new ArrayList<>();

            PassageResponse passageDto = null;

            for (TestQuestion tq : tqs) {
                Question q = questionRepository.findById(tq.getQuestionId()).orElse(null);
                if (q == null) continue;

                QuestionAdminResponse qDto = new QuestionAdminResponse();
                qDto.setQuestionId(q.getQuestionId());
                qDto.setExamPartId(q.getExamPartId());
                qDto.setQuestionText(q.getQuestionText());
                qDto.setQuestionType(q.getQuestionType());
                qDto.setExplanation(q.getExplanation());
                qDto.setClassId(q.getClassId());
                qDto.setTestPartId(part.getTestPartId());

                // ✅ Nếu có passage thì build PassageResponse đúng cấu trúc
                if (q.getPassageId() != null && passageDto == null) {
                    Passage passage = passageRepository.findById(q.getPassageId()).orElse(null);
                    if (passage != null) {
                        passageDto = new PassageResponse(
                                passage.getPassageId(),
                                passage.getContent(),
                                passage.getMediaUrl(),
                                passage.getPassageType()
                        );
                    }
                }

                // ✅ Gắn danh sách đáp án
                List<Answer> answers = answerRepository.findByQuestionId(q.getQuestionId());
                List<AnswerAdminResponse> answerDtos = answers.stream()
                        .map(a -> new AnswerAdminResponse(
                                a.getAnswerId(),
                                a.getAnswerText(),
                                a.getIsCorrect(),          // ✅ Gọi getter đúng cách
                                a.getAnswerLabel()
                        ))
                        .toList();

                qDto.setAnswers(answerDtos);
                questionDtos.add(qDto);
            }

            partDto.setQuestions(questionDtos);
            partDto.setPassage(passageDto);
            partResponses.add(partDto);
        }

        dto.setParts(partResponses);
        return dto;
    }

    @Transactional
    public TestResponse createTestForChapter(CreateChapterTestRequest request,
                                             MultipartFile bannerFile,
                                             HttpServletRequest httpRequest) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);

        Long classId = request.getClassId();
        Long chapterId = request.getChapterId();

        // ✅ Check teacher permission
        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        System.out.println("Teacher in DB = " + clazz.getTeacherId());
        System.out.println("Current user = " + currentUserId);

        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not teacher of this class"
            );
        }


        // ✅ Check chapter belongs to class
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        if (!chapter.getClassId().equals(classId)) {
            throw new RuntimeException("Chapter does not belong to this class");
        }

        // ============================
        // ✅ Create Test
        // ============================
        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(currentUserId);
        test.setCreatedAt(LocalDateTime.now());

        test.setDurationMinutes(request.getDurationMinutes());
        test.setAvailableFrom(parseDate(request.getAvailableFrom()));
        test.setAvailableTo(parseDate(request.getAvailableTo()));
        test.setMaxAttempts(request.getMaxAttempts());

        test.setClassId(classId);
        test.setChapterId(chapterId);

        if (bannerFile != null && !bannerFile.isEmpty()) {
            test.setBannerUrl(cloudinaryService.uploadImage(bannerFile));
        }

        testRepository.save(test);

        // ============================
        // ✅ Random questions per part
        // ============================
        for (ChapterPartRequest partReq : request.getParts()) {

            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getNumQuestions());

            testPartRepository.save(testPart);

            // random đúng chapter
            List<Question> randomQuestions =
                    questionRepository.findRandomQuestionsByExamPartIdAndClassIdAndChapterId(
                            partReq.getExamPartId(),
                            classId,
                            chapterId,
                            PageRequest.of(0, partReq.getNumQuestions())
                    );

            for (Question q : randomQuestions) {
                TestQuestion tq = new TestQuestion();
                tq.setTestPartId(testPart.getTestPartId());
                tq.setQuestionId(q.getQuestionId());
                testQuestionRepository.save(tq);
            }
        }

        return new TestResponse(test);
    }






}