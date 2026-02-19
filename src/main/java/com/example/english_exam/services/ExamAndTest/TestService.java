package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.AddQuestionsToTestRequest;
import com.example.english_exam.dto.request.AddRandomQuestionsToTestRequest;
import com.example.english_exam.dto.response.AddRandomQuestionsResponse;
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
import org.springframework.data.domain.Pageable;
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

        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng.");
        }

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // ================= AUTO SUBMIT IF TIME EXPIRED =================
        UserTest latest = userTestRepository
                .findTopByUserIdAndTestIdOrderByStartedAtDesc(currentUserId, testId)
                .orElse(null);

        if (latest != null && latest.getStatus() == UserTest.Status.IN_PROGRESS) {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = latest.getStartedAt()
                    .plusMinutes(test.getDurationMinutes());

            if (test.getAvailableTo() != null &&
                    test.getAvailableTo().isBefore(endTime)) {
                endTime = test.getAvailableTo();
            }

            if (!now.isBefore(endTime)) {
                try {
                    userTestService.submitTest(latest.getUserTestId());
                } catch (Exception e) {
                    latest.setStatus(UserTest.Status.COMPLETED);
                    latest.setFinishedAt(endTime);
                    userTestRepository.save(latest);
                }
            }
        }

        // ================= ATTEMPTS =================
        int attemptsUsed =
                userTestRepository.countByUserIdAndTestIdAndStatus(
                        currentUserId,
                        testId,
                        UserTest.Status.COMPLETED
                );

        Integer maxAttempts = test.getMaxAttempts();
        Integer remaining = (maxAttempts != null)
                ? Math.max(0, maxAttempts - attemptsUsed)
                : null;

        if (maxAttempts != null && remaining <= 0) {
            TestResponse blocked = new TestResponse(test);
            blocked.setCanDoTest(false);
            blocked.setAttemptsUsed(attemptsUsed);
            blocked.setRemainingAttempts(remaining);
            blocked.setStatus("FORBIDDEN");
            return blocked;
        }

        // ================= LOAD PARTS =================
        List<TestPart> testParts =
                testPartRepository.findByTestId(testId);

        if (testParts.isEmpty()) {
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
                    maxAttempts,
                    attemptsUsed,
                    remaining,
                    true,
                    Collections.emptyList()
            );
        }

        List<Long> partIds =
                testParts.stream()
                        .map(TestPart::getTestPartId)
                        .toList();

        List<TestQuestion> allTestQuestions =
                testQuestionRepository.findByTestPartIdIn(partIds);

        Map<Long, List<TestQuestion>> questionsByPart =
                allTestQuestions.stream()
                        .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        List<Long> questionIds =
                allTestQuestions.stream()
                        .map(TestQuestion::getQuestionId)
                        .distinct()
                        .toList();

        Map<Long, Question> questionMap =
                questionRepository.findAllById(questionIds)
                        .stream()
                        .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> passageIds =
                questionMap.values().stream()
                        .map(Question::getPassageId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, Passage> passageMap =
                passageIds.isEmpty()
                        ? Collections.emptyMap()
                        : passageRepository.findAllById(passageIds)
                        .stream()
                        .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        Map<Long, List<AnswerResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestions(questionIds);

        // ================= BUILD PART RESPONSES =================
        List<TestPartResponse> partResponses =
                testParts.stream().map(tp -> {

                    List<TestQuestion> tqList =
                            questionsByPart.getOrDefault(
                                    tp.getTestPartId(),
                                    Collections.emptyList()
                            );

                    Collections.shuffle(tqList);

                    List<QuestionResponse> questionResponses =
                            tqList.stream().map(tq -> {

                                        Question q =
                                                questionMap.get(tq.getQuestionId());
                                        if (q == null) return null;

                                        List<AnswerResponse> answers =
                                                answersByQuestionId.getOrDefault(
                                                        q.getQuestionId(),
                                                        Collections.emptyList()
                                                );

                                        PassageResponse passageDto = null;
                                        if (q.getPassageId() != null) {
                                            Passage p =
                                                    passageMap.get(q.getPassageId());
                                            if (p != null) {
                                                passageDto =
                                                        new PassageResponse(
                                                                p.getPassageId(),
                                                                p.getContent(),
                                                                p.getMediaUrl(),
                                                                p.getPassageType()
                                                        );
                                            }
                                        }

                                        return new QuestionResponse(
                                                q.getQuestionId(),
                                                q.getExamPartId(),
                                                q.getQuestionText(),
                                                q.getQuestionType(),
                                                q.getExplanation(),
                                                tp.getTestPartId(),
                                                answers,
                                                passageDto   // 👈 AUDIO PER QUESTION
                                        );

                                    })
                                    .filter(Objects::nonNull)
                                    .toList();

                    return new TestPartResponse(
                            tp.getTestPartId(),
                            tp.getExamPartId(),
                            tp.getNumQuestions(),
                            null,                 // ❌ không dùng passage cấp part
                            questionResponses
                    );

                }).toList();

        // ================= FINAL RESPONSE =================
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
                maxAttempts,
                attemptsUsed,
                remaining,
                true,
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

    /**
     * Lấy câu hỏi random từ kho và gắn vào part.
     * Cá nhân (không classId/chapterId): chỉ kho của user đăng nhập (created_by = currentUserId).
     * Lớp: classId (+ chapterId nếu có).
     */
    @Transactional
    public AddRandomQuestionsResponse addRandomQuestionsToTestPart(AddRandomQuestionsToTestRequest request, Long currentUserId) {
        if (request.getTestPartId() == null || request.getCount() == null || request.getCount() <= 0) {
            throw new RuntimeException("testPartId và count (số câu) phải hợp lệ.");
        }
        if (request.getChapterId() != null && request.getClassId() == null) {
            throw new RuntimeException("Khi có chapterId thì phải có classId.");
        }
        Long testPartId = request.getTestPartId();
        int count = request.getCount();
        TestPart testPart = testPartRepository.findById(testPartId)
                .orElseThrow(() -> new RuntimeException("TestPart không tồn tại: " + testPartId));
        Long examPartId = testPart.getExamPartId();

        Set<Long> existingIds = testQuestionRepository.findByTestPartId(testPartId).stream()
                .map(TestQuestion::getQuestionId)
                .collect(Collectors.toSet());

        List<Question> pool;
        if (request.getClassId() != null && request.getChapterId() != null) {
            pool = questionRepository.findRandomQuestionsByExamPartIdAndClassIdAndChapterId(
                    examPartId, request.getClassId(), request.getChapterId(), Pageable.ofSize(count));
        } else if (request.getClassId() != null) {
            pool = questionRepository.findRandomQuestionsByExamPartIdAndClassId(
                    examPartId, request.getClassId(), Pageable.ofSize(count));
        } else {
            pool = questionRepository.findRandomByExamPartAndCreatedByAndClassIdIsNullAndChapterIdIsNull(
                    examPartId, currentUserId, count);
        }

        List<Long> toAdd = pool.stream()
                .map(Question::getQuestionId)
                .filter(id -> !existingIds.contains(id))
                .limit(count)
                .toList();

        for (Long questionId : toAdd) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại: " + questionId));
            if (!question.getExamPartId().equals(examPartId)) {
                continue;
            }
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(testPartId);
            tq.setQuestionId(questionId);
            testQuestionRepository.save(tq);
        }
        return new AddRandomQuestionsResponse(toAdd.size());
    }









}