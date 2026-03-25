package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.request.AddQuestionsToTestRequest;
import com.example.english_exam.dto.request.AddRandomQuestionsToTestRequest;
import com.example.english_exam.dto.request.CreateTestRequest;
import com.example.english_exam.dto.response.AddRandomQuestionsResponse;
import com.example.english_exam.dto.response.*;
import com.example.english_exam.dto.response.admin.*;
import com.example.english_exam.dto.response.user.*;
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
    private final PassageRepository  passageRepository;
    private final UserTestRepository userTestRepository;
    private final AuthUtils authUtils;
    private final UserTestService userTestService;
    private final ClassRepository classRepository;
    private final ClassMemberRepository classMemberRepository;


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

    /**
     * Cập nhật test từ CreateTestRequest (dùng chung DTO với tạo mới).
     * Chỉ ghi đè các field được gửi lên (khác null); không đổi createdBy, createdAt.
     */
    public Test updateTest(Long id, CreateTestRequest request) {

        Test test = testRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test không tồn tại: " + id));
        if (request.getTitle() != null) test.setTitle(request.getTitle());
        if (request.getDescription() != null) test.setDescription(request.getDescription());
        if (request.getExamTypeId() != null) test.setExamTypeId(request.getExamTypeId());
        if (request.getDurationMinutes() != null) test.setDurationMinutes(request.getDurationMinutes());
        if (request.getBannerUrl() != null) test.setBannerUrl(request.getBannerUrl());
        if (request.getMaxAttempts() != null) test.setMaxAttempts(request.getMaxAttempts());
        if (request.getClassId() != null) test.setClassId(request.getClassId());
        if (request.getChapterId() != null) test.setChapterId(request.getChapterId());
        if (request.getAvailableFrom() != null) test.setAvailableFrom(request.getAvailableFrom());
        if (request.getAvailableTo() != null) test.setAvailableTo(request.getAvailableTo());
        return testRepository.save(test);
    }

    public TestResponse buildUserTestSummary(Test test, Long userId) {

        long attemptsUsed =
                userTestRepository.countByTestIdAndUserId(
                        test.getTestId(),
                        userId
                );
        long totalAttempts = userTestRepository.countByTestId(test.getTestId());

        Integer maxAttempts = test.getMaxAttempts();
        Integer remainingAttempts = null;
        boolean canDoTest = true;

        if (maxAttempts != null) {
            remainingAttempts = (int) Math.max(0, maxAttempts - attemptsUsed);
            canDoTest = remainingAttempts > 0;
        }

        return TestResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTypeId(test.getExamTypeId())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .bannerUrl(test.getBannerUrl())
                .durationMinutes(test.getDurationMinutes())
                .availableFrom(test.getAvailableFrom())
                .availableTo(test.getAvailableTo())
                .status(test.calculateStatus().name())
                .maxAttempts(maxAttempts)                // giữ nguyên null
                .attemptsUsed((int) attemptsUsed)
                .remainingAttempts(remainingAttempts)   // null nếu không giới hạn
                .totalAttempts(totalAttempts)
                .canDoTest(canDoTest)                   // luôn true nếu null
                .parts(null)
                .build();
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

    private TestAdminResponse buildAdminTestSummary(Test test) {
        long totalAttempts = userTestRepository.countByTestId(test.getTestId());

        return TestAdminResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTypeId(test.getExamTypeId())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .bannerUrl(test.getBannerUrl())
                .durationMinutes(test.getDurationMinutes())
                .availableFrom(test.getAvailableFrom())
                .availableTo(test.getAvailableTo())
                .status(test.calculateStatus().name())
                .maxAttempts(test.getMaxAttempts())
                .totalAttempts(totalAttempts)
                .classId(test.getClassId())
                .parts(null)
                .build();
    }

    public List<TestAdminResponse> getTestsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return testRepository.findByCreatedBy(userId).stream()
                .map(this::buildAdminTestSummary)
                .toList();
    }

    public List<TestResponse> getTestsByUser(HttpServletRequest httpRequest) {
        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng.");
        }
        return testRepository.findByCreatedBy(currentUserId).stream()
                .map(test -> buildUserTestSummary(test, currentUserId))
                .toList();
    }

    @Transactional
    public TestResponse getTestFullById(Long testId, HttpServletRequest httpRequest) {

        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng.");
        }

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        long totalAttempts = userTestRepository.countByTestId(testId);

        // ================= AUTO SUBMIT IF TIME EXPIRED =================
        UserTest latest = userTestRepository
                .findTopByUserIdAndTestIdOrderByStartedAtDesc(currentUserId, testId)
                .orElse(null);

        Integer duration = test.getDurationMinutes();

// Chỉ xử lý auto submit khi có giới hạn thời gian hợp lệ
        if (latest != null
                && latest.getStatus() == UserTest.Status.IN_PROGRESS
                && duration != null
                && duration > 0) {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = latest.getStartedAt().plusMinutes(duration);

            // Nếu có availableTo và nó sớm hơn endTime → dùng availableTo
            if (test.getAvailableTo() != null
                    && test.getAvailableTo().isBefore(endTime)) {
                endTime = test.getAvailableTo();
            }

            // Nếu đã quá hạn → auto submit
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
        int attemptsUsed = userTestRepository.countByUserIdAndTestIdAndStatus(
                currentUserId, testId, UserTest.Status.COMPLETED);

        Integer maxAttempts = test.getMaxAttempts();
        Integer remaining = (maxAttempts != null) ? Math.max(0, maxAttempts - attemptsUsed) : null;

        if (maxAttempts != null && remaining <= 0) {

            return TestResponse.builder()
                    .testId(test.getTestId())
                    .title(test.getTitle())
                    .description(test.getDescription())
                    .examTypeId(test.getExamTypeId())
                    .createdBy(test.getCreatedBy())
                    .createdAt(test.getCreatedAt())
                    .bannerUrl(test.getBannerUrl())
                    .durationMinutes(test.getDurationMinutes())
                    .availableFrom(test.getAvailableFrom())
                    .availableTo(test.getAvailableTo())
                    .status("FORBIDDEN")
                    .maxAttempts(maxAttempts)
                    .attemptsUsed(attemptsUsed)
                    .remainingAttempts(remaining)
                    .totalAttempts(totalAttempts)
                    .canDoTest(false)
                    .parts(null)
                    .build();
        }

        // ================= LOAD DATA HÀNG LOẠT =================
        List<TestPart> testParts = testPartRepository.findByTestId(testId);
        if (testParts.isEmpty()) {
            return buildEmptyTestResponse(test, maxAttempts, attemptsUsed, remaining);
        }

        List<Long> partIds = testParts.stream().map(TestPart::getTestPartId).toList();
        List<TestQuestion> allTestQuestions = testQuestionRepository.findByTestPartIdIn(partIds);
        Map<Long, List<TestQuestion>> questionsByPart = allTestQuestions.stream()
                .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        List<Long> questionIds = allTestQuestions.stream().map(TestQuestion::getQuestionId).distinct().toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> passageIds = questionMap.values().stream()
                .map(Question::getPassageId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Passage> passageMap = passageIds.isEmpty() ? Collections.emptyMap() :
                passageRepository.findAllById(passageIds).stream()
                        .collect(Collectors.toMap(Passage::getPassageId, p -> p));

        Map<Long, List<AnswerResponse>> answersByQuestionId = answerService.getAnswersForMultipleQuestions(questionIds);

        // ================= BUILD RESPONSES (WITH GROUPING) =================
        List<TestPartResponse> partResponses = testParts.stream().map(tp -> {
            List<TestQuestion> tqList = questionsByPart.getOrDefault(tp.getTestPartId(), Collections.emptyList());

            // Map để gom các câu hỏi vào nhóm theo passageId
            // Key là passageId, hoặc questionId nếu là câu độc lập
            Map<String, QuestionGroupResponse> groupsMap = new LinkedHashMap<>();

            for (TestQuestion tq : tqList) {
                Question q = questionMap.get(tq.getQuestionId());
                if (q == null) continue;

                List<AnswerResponse> answers = answersByQuestionId.getOrDefault(q.getQuestionId(), Collections.emptyList());

                // Build Question DTO (để passage = null để tránh lặp dữ liệu trong JSON)
                // Khởi tạo cho User
                QuestionResponse qDto = QuestionResponse.builder()
                        .questionId(q.getQuestionId())
                        .examPartId(q.getExamPartId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .isBank(q.getIsBank())
                        .testPartId(tp.getTestPartId())
                        .answers(answers)
                        .build();

                if (q.getPassageId() != null) {
                    // Nhóm theo Passage
                    String groupKey = "P_" + q.getPassageId();
                    if (!groupsMap.containsKey(groupKey)) {
                        Passage p = passageMap.get(q.getPassageId());
                        PassageResponse pDto = (p != null) ? new PassageResponse(
                                p.getPassageId(), p.getContent(), p.getMediaUrl(), p.getPassageType()) : null;
                        groupsMap.put(groupKey, new QuestionGroupResponse(pDto, new ArrayList<>()));
                    }
                    groupsMap.get(groupKey).getQuestions().add(qDto);
                } else {
                    // Câu hỏi độc lập -> Mỗi câu là 1 nhóm riêng
                    String groupKey = "Q_" + q.getQuestionId();
                    groupsMap.put(groupKey, new QuestionGroupResponse(null, new ArrayList<>(List.of(qDto))));
                }
            }

            // Chuyển Map thành List và SHUFFLE CÁC NHÓM
            List<QuestionGroupResponse> finalGroups = new ArrayList<>(groupsMap.values());
            Collections.shuffle(finalGroups); // Chỉ shuffle các nhóm để đảm bảo câu hỏi cùng passage ko bị tách ra

            return new TestPartResponse(tp.getTestPartId(), tp.getExamPartId(), finalGroups);
        }).toList();

        return TestResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTypeId(test.getExamTypeId())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .bannerUrl(test.getBannerUrl())
                .durationMinutes(test.getDurationMinutes())
                .availableFrom(test.getAvailableFrom())
                .availableTo(test.getAvailableTo())
                .status(test.calculateStatus().name())
                .maxAttempts(maxAttempts)
                .attemptsUsed(attemptsUsed)
                .remainingAttempts(remaining)
                .totalAttempts(totalAttempts)
                .canDoTest(true)
                .parts(partResponses)
                .build();
    }



    // Hàm bổ trợ để build response trống
    private TestResponse buildEmptyTestResponse(Test test, Integer maxAttempts, int attemptsUsed, Integer remaining) {
        long totalAttempts = userTestRepository.countByTestId(test.getTestId());
        return TestResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTypeId(test.getExamTypeId())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .bannerUrl(test.getBannerUrl())
                .durationMinutes(test.getDurationMinutes())
                .availableFrom(test.getAvailableFrom())
                .availableTo(test.getAvailableTo())
                .status(test.calculateStatus().name())
                .maxAttempts(maxAttempts)
                .attemptsUsed(attemptsUsed)
                .remainingAttempts(remaining)
                .totalAttempts(totalAttempts)
                .canDoTest(true)
                .parts(Collections.emptyList())
                .build();
    }


    public TestAdminResponse getTestFullByIdAdmin(Long testId) {

        // ===== 1. LẤY TEST =====
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        long totalAttempts = userTestRepository.countByTestId(testId);

        // ===== 2. LẤY TEST PARTS =====
        List<TestPart> testParts = testPartRepository.findByTestId(test.getTestId());

        if (testParts.isEmpty()) {
            return TestAdminResponse.builder()
                    .testId(test.getTestId())
                    .title(test.getTitle())
                    .description(test.getDescription())
                    .examTypeId(test.getExamTypeId())
                    .createdBy(test.getCreatedBy())
                    .createdAt(test.getCreatedAt())
                    .bannerUrl(test.getBannerUrl())
                    .durationMinutes(test.getDurationMinutes())
                    .availableFrom(test.getAvailableFrom())
                    .availableTo(test.getAvailableTo())
                    .status(test.calculateStatus().name())
                    .maxAttempts(test.getMaxAttempts())
                    .totalAttempts(totalAttempts)
                    .classId(test.getClassId())
                    .parts(Collections.emptyList())
                    .build();
        }

        List<Long> testPartIds = testParts.stream()
                .map(TestPart::getTestPartId)
                .toList();

        // ===== 3. LOAD ALL TEST QUESTIONS =====
        List<TestQuestion> allTestQuestions =
                testQuestionRepository.findByTestPartIdIn(testPartIds);

        Map<Long, List<TestQuestion>> questionsByPartId =
                allTestQuestions.stream()
                        .collect(Collectors.groupingBy(TestQuestion::getTestPartId));

        // ===== 4. LOAD ALL QUESTIONS =====
        List<Long> allQuestionIds = allTestQuestions.stream()
                .map(TestQuestion::getQuestionId)
                .toList();

        Map<Long, Question> questionMap =
                questionRepository.findAllById(allQuestionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Question::getQuestionId,
                                q -> q
                        ));

        // ===== 5. LOAD ALL PASSAGES =====
        Set<Long> allPassageIds = questionMap.values().stream()
                .map(Question::getPassageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Passage> passageMap =
                passageRepository.findAllById(allPassageIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Passage::getPassageId,
                                p -> p
                        ));

        // ===== 6. LOAD ALL EXAM PARTS (FIX N+1) =====
        Set<Long> examPartIds = questionMap.values().stream()
                .map(Question::getExamPartId)
                .collect(Collectors.toSet());

        Map<Long, ExamPart> examPartMap =
                examPartRepository.findAllById(examPartIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ExamPart::getExamPartId,
                                e -> e
                        ));

        // ===== 7. LOAD ALL ANSWERS =====
        Map<Long, List<AnswerAdminResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestionsForAdmin(allQuestionIds);

        // ===== 8. BUILD RESPONSE IN MEMORY =====
        List<TestPartAdminResponse> partResponses = testParts.stream()
                .map(tp -> {

                    List<TestQuestion> tqList =
                            questionsByPartId.getOrDefault(
                                    tp.getTestPartId(),
                                    Collections.emptyList()
                            );

                    // ===== GROUP QUESTIONS BY PASSAGE =====
                    Map<Long, List<Question>> groupedByPassage =
                            tqList.stream()
                                    .map(tq -> questionMap.get(tq.getQuestionId()))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.groupingBy(
                                            q -> q.getPassageId() == null
                                                    ? -1L
                                                    : q.getPassageId()
                                    ));

                    List<QuestionGroupAdminResponse> groupResponses =
                            groupedByPassage.entrySet()
                                    .stream()
                                    .map(entry -> {

                                        Long passageId = entry.getKey();
                                        List<Question> questionsInGroup = entry.getValue();

                                        // ===== MAP PASSAGE =====
                                        PassageResponse passageResponse = null;

                                        if (!passageId.equals(-1L)) {
                                            Passage p = passageMap.get(passageId);
                                            if (p != null) {
                                                passageResponse = new PassageResponse(
                                                        p.getPassageId(),
                                                        p.getContent(),
                                                        p.getMediaUrl(),
                                                        p.getPassageType()
                                                );
                                            }
                                        }

                                        // ===== MAP QUESTIONS =====
                                        List<QuestionAdminResponse> questionResponses =
                                                questionsInGroup.stream()
                                                        .map(q -> {

                                                            List<AnswerAdminResponse> answers =
                                                                    answersByQuestionId.getOrDefault(
                                                                            q.getQuestionId(),
                                                                            Collections.emptyList()
                                                                    );

                                                            Long examTypeId =
                                                                    Optional.ofNullable(
                                                                                    examPartMap.get(q.getExamPartId())
                                                                            )
                                                                            .map(ExamPart::getExamTypeId)
                                                                            .orElse(null);

                                                            return QuestionAdminResponse.builder()
                                                                    .questionId(q.getQuestionId())
                                                                    .examPartId(q.getExamPartId())
                                                                    .questionText(q.getQuestionText())
                                                                    .questionType(q.getQuestionType())
                                                                    .explanation(q.getExplanation())
                                                                    .examTypeId(examTypeId)
                                                                    .classId(q.getClassId())
                                                                    .isBank(q.getIsBank())
                                                                    .answers(answers)
                                                                    .build();
                                                        })
                                                        .toList();

                                        return new QuestionGroupAdminResponse(
                                                passageResponse,
                                                questionResponses
                                        );
                                    })
                                    .toList();

                    return new TestPartAdminResponse(
                            tp.getTestPartId(),
                            tp.getExamPartId(),
                            groupResponses
                    );
                })
                .toList();

        // ===== 9. RETURN FINAL RESPONSE =====
        return TestAdminResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTypeId(test.getExamTypeId())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .bannerUrl(test.getBannerUrl())
                .durationMinutes(test.getDurationMinutes())
                .availableFrom(test.getAvailableFrom())
                .availableTo(test.getAvailableTo())
                .status(test.calculateStatus().name())
                .maxAttempts(test.getMaxAttempts())
                .totalAttempts(totalAttempts)
                .classId(test.getClassId())
                .parts(partResponses)
                .build();
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