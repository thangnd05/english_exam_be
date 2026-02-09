package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.AddQuestionsToTestRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public TestResponse buildUserTestSummary(Test test, Long userId) {

        TestResponse response = new TestResponse(test);

        long attemptsUsed =
                userTestRepository.countByTestIdAndUserId(
                        test.getTestId(),
                        userId
                );

        int maxAttempts =
                test.getMaxAttempts() == null ? 1 : test.getMaxAttempts();

        int remainingAttempts =
                (int) Math.max(0, maxAttempts - attemptsUsed);

        response.setAttemptsUsed((int) attemptsUsed);
        response.setRemainingAttempts(remainingAttempts);
        response.setCanDoTest(remainingAttempts > 0);

        return response;
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
                        examTypeId,
                        q.getExamPartId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        passageDto,
                        tp.getTestPartId(),
                        answers,
                        q.getClassId(),
                        q.getIsBank()
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

    public List<Test> getTestByClassIdAndChapterId(Long classId,Long chapterId, HttpServletRequest request) {
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
        return testRepository.findByClassIdAndChapterId(classId,chapterId);
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

    public List<TestResponse> getMyPersonalTests(HttpServletRequest request) {

        Long currentUserId = authUtils.getUserId(request);
        if (currentUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "🔒 Bạn cần đăng nhập để xem bài kiểm tra."
            );
        }

        List<Test> tests =
                testRepository.findByCreatedByAndClassIdIsNullAndChapterIdIsNull(currentUserId);

        return tests.stream()
                .map(test -> buildUserTestSummary(test, currentUserId))
                .toList();
    }




    /**
     * Gắn câu hỏi từ kho vào part của đề (chỉ tạo bản ghi test_questions).
     * Câu hỏi phải đã tồn tại trong kho; không tạo câu hỏi mới ở đây.
     */
    @Transactional
    public void addQuestionsToTestPart(AddQuestionsToTestRequest request) {
        if (request.getTestPartId() == null || request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            throw new RuntimeException("testPartId và questionIds không được rỗng.");
        }
        Long testPartId = request.getTestPartId();
        TestPart testPart = testPartRepository.findById(testPartId)
                .orElseThrow(() -> new RuntimeException("TestPart không tồn tại: " + testPartId));

        for (Long questionId : request.getQuestionIds()) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại trong kho: " + questionId));
            if (!question.getExamPartId().equals(testPart.getExamPartId())) {
                throw new RuntimeException("Câu hỏi " + questionId + " không thuộc examPart của part này.");
            }
            if (testQuestionRepository.existsByQuestionIdAndTestPartId(questionId, testPartId)) {
                continue;
            }
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(testPartId);
            tq.setQuestionId(questionId);
            testQuestionRepository.save(tq);
        }
    }











}