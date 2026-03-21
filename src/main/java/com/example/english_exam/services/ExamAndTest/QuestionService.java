package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.*;
import com.example.english_exam.dto.response.PassageMediaResponse;
import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@AllArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final PassageRepository passageRepository;
    private final ExamPartRepository examPartRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestPartRepository testPartRepository;
    private final CloudinaryService cloudinaryService;
    private final AuthUtils authUtils;
    private final PassageMediaRepository passageMediaRepository;
    private final AnswerService answerService;




    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    /**
     * Lấy danh sách câu theo part.
     * Cá nhân (classId == null): chỉ câu của user đăng nhập (created_by = currentUserId, class_id/chapter_id NULL).
     * Lớp: classId (+ chapterId nếu có).
     */
    private List<Question> fetchQuestions(
            Long examPartId,
            Long classId,
            Long chapterId,
            Long currentUserId
    ) {

        if (classId == null) {
            return questionRepository
                    .findByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNullAndIsBankTrue(
                            examPartId, currentUserId);
        }

        if (chapterId != null) {
            return questionRepository
                    .findByExamPartIdAndClassIdAndChapterId(
                            examPartId, classId, chapterId);
        }

        return questionRepository
                .findByExamPartIdAndClassId(
                        examPartId, classId);
    }

    public List<QuestionResponse> getQuestionsByPart(
            Long examPartId,
            Long classId,
            Long chapterId,
            HttpServletRequest request
    ) {

        Long currentUserId = authUtils.getUserId(request);

        List<Question> questions = fetchQuestions(
                examPartId,
                classId,
                chapterId,
                currentUserId
        );

        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        // ===== BULK LOAD ANSWERS (FIX N+1) =====
        List<Long> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .toList();

        Map<Long, List<AnswerResponse>> answersByQuestionId =
                answerService.getAnswersForMultipleQuestions(questionIds);

        // ===== MAP USING BUILDER =====
        return questions.stream()
                .map(q -> QuestionResponse.builder()
                        .questionId(q.getQuestionId())
                        .examPartId(q.getExamPartId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .answers(
                                answersByQuestionId.getOrDefault(
                                        q.getQuestionId(),
                                        Collections.emptyList()
                                )
                        )
                        .build()
                )
                .toList();
    }



    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }

    /**
     * Đếm câu theo part. Cá nhân = theo user đăng nhập; lớp = classId (+ chapterId nếu có).
     */
    public long countByExamPartId(Long examPartId, Long classId, Long chapterId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);
        if (classId != null) {
            if (chapterId != null) {
                return questionRepository.countByExamPartIdAndClassIdAndChapterId(examPartId, classId, chapterId);
            }
            return questionRepository.countByExamPartIdAndClassId(examPartId, classId);
        }
        return questionRepository.countByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNullAndIsBankTrue(examPartId, currentUserId);
    }


    /**
     * Tạo nhiều câu hỏi cùng 1 passage vào kho (BẮT BUỘC lưu kho, BẮT BUỘC có passage).
     * Passage trong request là bắt buộc cho bulk; nếu LISTENING có thể kèm file audio.
     */
    @Transactional
    public List<QuestionAdminResponse> createBulkQuestionsToBank(BulkQuestionWithPassageRequest request,
                                                                HttpServletRequest httpRequest,
                                                                MultipartFile audioFile) throws IOException {
        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng từ token.");
        }
        if (request.getPassage() == null || (request.getPassage().getContent() == null || request.getPassage().getContent().trim().isEmpty())
                && (request.getPassage().getPassageType() != Passage.PassageType.LISTENING || audioFile == null || audioFile.isEmpty())) {
            throw new RuntimeException("Bulk tạo câu hỏi theo đoạn bắt buộc phải có passage (nội dung hoặc audio cho LISTENING).");
        }

        Passage passage = new Passage();
        passage.setContent(request.getPassage().getContent() != null ? request.getPassage().getContent() : "");
        passage.setPassageType(request.getPassage().getPassageType());
        if (passage.getPassageType() == Passage.PassageType.LISTENING && audioFile != null && !audioFile.isEmpty()) {
            passage.setMediaUrl(cloudinaryService.uploadAudio(audioFile));
        } else {
            passage.setMediaUrl(request.getPassage().getMediaUrl());
        }
        passage = passageRepository.save(passage);
        Long passageId = passage.getPassageId();

        List<QuestionAdminResponse> responses = new ArrayList<>();
        for (NormalQuestionRequest qReq : request.getQuestions()) {
            Question question = new Question();
            question.setExamPartId(request.getExamPartId());
            question.setPassageId(passageId);
            question.setQuestionText(qReq.getQuestionText());
            question.setQuestionType(qReq.getQuestionType());
            question.setCreatedBy(currentUserId);
            if (request.getClassId() != null) question.setClassId(request.getClassId());
            if (request.getChapterId() != null) question.setChapterId(request.getChapterId());
            question.setIsBank(Boolean.TRUE);
            question = questionRepository.save(question);

            List<Answer> savedAnswers = saveAnswersForQuestion(question.getQuestionId(), qReq.getAnswers(), qReq.getQuestionType());
            responses.add(buildQuestionAdminResponse(question, passage, savedAnswers));
        }
        return responses;
    }

    /**
     * Tạo nhiều câu hỏi thông thường vào kho (không passage). Mỗi câu độc lập.
     */
    @Transactional
    public List<QuestionAdminResponse> createBulkQuestionsToBankNoPassage(
            BulkCreateQuestionsToBankRequest request,
            HttpServletRequest httpRequest,
            Map<String, MultipartFile> files
    ) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng từ token.");
        }

        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            return List.of();
        }

        List<QuestionAdminResponse> responses = new ArrayList<>();

        for (int i = 0; i < request.getQuestions().size(); i++) {

            final int questionIndex = i;
            NormalQuestionRequest qReq = request.getQuestions().get(i);

            Question question = new Question();
            question.setExamPartId(request.getExamPartId());
            question.setPassageId(null);
            question.setQuestionText(qReq.getQuestionText());
            question.setQuestionType(qReq.getQuestionType());
            question.setCreatedBy(currentUserId);
            question.setIsBank(Boolean.TRUE);

            if (request.getClassId() != null)
                question.setClassId(request.getClassId());

            if (request.getChapterId() != null)
                question.setChapterId(request.getChapterId());

            question = questionRepository.save(question);

            // 🔥 Lấy toàn bộ file của câu hỏi này
            List<MultipartFile> questionFiles = files.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("media_" + questionIndex + "_"))
                    .map(Map.Entry::getValue)
                    .toList();

            if (!questionFiles.isEmpty()) {

                // 1. Tạo đối tượng Passage mới
                Passage passage = new Passage();
                passage.setContent("");

                // --- ĐOẠN SỬA ĐỂ NHẬN DIỆN LOẠI ---
                // Mặc định là READING (cho ảnh), nếu thấy file audio thì chuyển thành LISTENING
                Passage.PassageType determinedType = Passage.PassageType.READING;

                for (MultipartFile file : questionFiles) {
                    if (file != null && file.getContentType() != null) {
                        if (file.getContentType().startsWith("audio")) {
                            determinedType = Passage.PassageType.LISTENING;
                            break; // Ưu tiên LISTENING nếu có file âm thanh
                        }
                    }
                }

                passage.setPassageType(determinedType);
                // ---------------------------------

                passage = passageRepository.save(passage);

                question.setPassageId(passage.getPassageId());
                questionRepository.save(question);

                for (MultipartFile file : questionFiles) {
                    if (file == null || file.isEmpty()) continue;

                    String uploadedUrl;
                    PassageMedia.MediaType mediaType;

                    // Tối ưu: Dựa vào ContentType của file để gọi Cloudinary tương ứng
                    if (file.getContentType() != null && file.getContentType().startsWith("audio")) {
                        uploadedUrl = cloudinaryService.uploadAudio(file);
                        mediaType = PassageMedia.MediaType.AUDIO;
                    } else {
                        uploadedUrl = cloudinaryService.uploadImage(file);
                        mediaType = PassageMedia.MediaType.IMAGE;
                    }

                    PassageMedia media = new PassageMedia();
                    media.setPassageId(passage.getPassageId());
                    media.setMediaUrl(uploadedUrl);
                    media.setMediaType(mediaType);

                    passageMediaRepository.save(media);
                }
            }

            List<Answer> savedAnswers = saveAnswersForQuestion(
                    question.getQuestionId(),
                    qReq.getAnswers(),
                    qReq.getQuestionType()
            );

            responses.add(
                    buildQuestionAdminResponse(question, null, savedAnswers)
            );
        }

        return responses;
    }



    /**
     * Tạo câu hỏi "tức thì" (isBank = false) và gắn thẳng vào part của đề.
     * Dùng khi giáo viên trên lớp đặt câu hỏi rồi đưa vào đề, không cần lưu kho.
     */
    @Transactional
    public QuestionAdminResponse createQuestionAndAttachToTest(
            CreateQuestionAndAttachRequest request,
            HttpServletRequest httpRequest,
            Map<String, MultipartFile> files // Chuyển sang Map để nhận đa file
    ) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng.");
        }

        TestPart testPart = testPartRepository.findById(request.getTestPartId())
                .orElseThrow(() -> new RuntimeException("TestPart không tồn tại: " + request.getTestPartId()));

        Long passageId = null;
        Passage savedPassage = null;

        // Kiểm tra xem có file nào gửi lên không hoặc có content passage không
        boolean hasFiles = files != null && !files.isEmpty();
        boolean hasPassageReq = request.getPassage() != null;

        if (hasFiles || (hasPassageReq && hasPassageContent(request.getPassage()))) {
            Passage passage = new Passage();
            if (hasPassageReq) {
                passage.setContent(request.getPassage().getContent() != null ? request.getPassage().getContent() : "");
                passage.setPassageType(request.getPassage().getPassageType());
            } else {
                // Mặc định nếu chỉ có file mà không có request passage
                passage.setContent("");
                passage.setPassageType(Passage.PassageType.LISTENING);
            }

            savedPassage = passageRepository.save(passage);
            passageId = savedPassage.getPassageId();

            // 🔥 XỬ LÝ ĐA PHƯƠNG TIỆN (Lưu vào passage_media)
            // 🔥 XỬ LÝ ĐA PHƯƠNG TIỆN (Lưu vào passage_media)
            if (hasFiles) {
                for (MultipartFile file : files.values()) {

                    if (file == null || file.isEmpty()) continue;

                    String contentType = file.getContentType();

                    String uploadedUrl;
                    PassageMedia.MediaType mediaType;

                    if (contentType != null && contentType.startsWith("image")) {
                        uploadedUrl = cloudinaryService.uploadImage(file);
                        mediaType = PassageMedia.MediaType.IMAGE;
                    } else if (contentType != null && contentType.startsWith("audio")) {
                        uploadedUrl = cloudinaryService.uploadAudio(file);
                        mediaType = PassageMedia.MediaType.AUDIO;
                    } else {
                        // PDF / DOC / XLS ...
                        uploadedUrl = cloudinaryService.uploadDocument(file);
                        mediaType = PassageMedia.MediaType.DOCUMENT;
                    }

                    PassageMedia media = new PassageMedia();
                    media.setPassageId(passageId);
                    media.setMediaUrl(uploadedUrl);
                    media.setMediaType(mediaType);

                    passageMediaRepository.save(media);
                }
            }
        }

            // Lưu Question
        Question question = new Question();
        question.setExamPartId(testPart.getExamPartId());
        question.setPassageId(passageId);
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setCreatedBy(currentUserId);
        question.setIsBank(Boolean.FALSE);

        if (request.getClassId() != null) question.setClassId(request.getClassId());
        if (request.getChapterId() != null) question.setChapterId(request.getChapterId());

        question = questionRepository.save(question);

        // Lưu Answers
        List<Answer> savedAnswers = saveAnswersForQuestion(
                question.getQuestionId(),
                request.getAnswers(),
                request.getQuestionType()
        );

        // Gán vào Test
        TestQuestion tq = new TestQuestion();
        tq.setTestPartId(request.getTestPartId());
        tq.setQuestionId(question.getQuestionId());
        testQuestionRepository.save(tq);

        return buildQuestionAdminResponse(question, savedPassage, savedAnswers);
    }

    private boolean hasPassageContent(PassageRequest pr) {
        return (pr.getContent() != null && !pr.getContent().trim().isEmpty()) || pr.getMediaUrl() != null;
    }

    private List<Answer> saveAnswersForQuestion(
            Long questionId,
            List<AnswerRequest> answers,
            Question.QuestionType questionType
    ) {

        if (answers == null || answers.isEmpty()) return List.of();

        List<Answer> list = new ArrayList<>();

        if (questionType == Question.QuestionType.FILL_BLANK) {

            AnswerRequest ar = answers.get(0);

            Answer a = new Answer();
            a.setQuestionId(questionId);
            a.setAnswerText(ar.getAnswerText() != null ? ar.getAnswerText() : "");
            a.setAnswerLabel(ar.getAnswerLabel() != null ? ar.getAnswerLabel() : "");
            a.setIsCorrect(true);

            list.add(a);

        } else {

            for (AnswerRequest ar : answers) {

                Answer a = new Answer();
                a.setQuestionId(questionId);

                // 🔥 FIX 1: đảm bảo không null
                a.setAnswerText(ar.getAnswerText() != null ? ar.getAnswerText() : "");

                // 🔥 FIX 2: đảm bảo không null
                a.setAnswerLabel(ar.getAnswerLabel() != null ? ar.getAnswerLabel() : "");

                // 🔥 FIX 3: đảm bảo boolean không null
                a.setIsCorrect(Boolean.TRUE.equals(ar.getIsCorrect()));

                list.add(a);
            }
        }

        return answerRepository.saveAll(list);
    }


    private List<PassageMediaResponse> toPassageMediaResponses(Long passageId) {
        if (passageId == null) {
            return List.of();
        }
        return passageMediaRepository.findByPassageId(passageId).stream()
                .map(m -> new PassageMediaResponse(
                        m.getId(),
                        m.getPassageId(),
                        m.getMediaUrl(),
                        m.getMediaType().name()
                ))
                .toList();
    }

    /**
     * Upload thêm file vào passage (append vào passage_media). Giống luồng create-and-attach.
     */
    private void appendUploadedFilesToPassage(Long passageId, Collection<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            String uploadedUrl;
            PassageMedia.MediaType mediaType;
            if (contentType != null && contentType.startsWith("image")) {
                uploadedUrl = cloudinaryService.uploadImage(file);
                mediaType = PassageMedia.MediaType.IMAGE;
            } else if (contentType != null && contentType.startsWith("audio")) {
                uploadedUrl = cloudinaryService.uploadAudio(file);
                mediaType = PassageMedia.MediaType.AUDIO;
            } else {
                uploadedUrl = cloudinaryService.uploadDocument(file);
                mediaType = PassageMedia.MediaType.DOCUMENT;
            }
            PassageMedia media = new PassageMedia();
            media.setPassageId(passageId);
            media.setMediaUrl(uploadedUrl);
            media.setMediaType(mediaType);
            passageMediaRepository.save(media);
        }
    }

    private QuestionAdminResponse buildQuestionAdminResponse(Question question, Passage passage,
                                                             List<Answer> answerEntities) {
        Long examTypeId = examPartRepository.findById(question.getExamPartId())
                .map(ExamPart::getExamTypeId).orElse(null);
        PassageResponse passageDto = null;
        if (passage != null) {
            passageDto = new PassageResponse(
                    passage.getPassageId(),
                    passage.getContent(),
                    passage.getMediaUrl(),
                    passage.getPassageType()
            );
        }
        List<PassageMediaResponse> passageMedia = passage != null
                ? toPassageMediaResponses(passage.getPassageId())
                : List.of();
        List<AnswerAdminResponse> answerDtos = answerEntities.stream()
                .map(a -> AnswerAdminResponse.builder()
                        .answerId(a.getAnswerId())
                        .answerText(a.getAnswerText())
                        .answerLabel(a.getAnswerLabel())
                        .isCorrect(a.getIsCorrect())
                        .build()
                )
                .toList();
        return QuestionAdminResponse.builder()
                .questionId(question.getQuestionId())
                .examPartId(question.getExamPartId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .explanation(question.getExplanation())
                .examTypeId(examTypeId)
                .classId(question.getClassId())
                .isBank(question.getIsBank())
                .passage(passageDto)
                .passageMedia(passageMedia)
                .answers(answerDtos)
                .build();
    }

    public QuestionAdminResponse getQuestionDetailAdmin(Long questionId) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Long examTypeId = examPartRepository.findById(question.getExamPartId())
                .map(ExamPart::getExamTypeId)
                .orElse(null);

        Passage passageEntity = Optional.ofNullable(question.getPassageId())
                .flatMap(passageRepository::findById)
                .orElse(null);
        PassageResponse passageDto = passageEntity != null
                ? new PassageResponse(
                passageEntity.getPassageId(),
                passageEntity.getContent(),
                passageEntity.getMediaUrl(),
                passageEntity.getPassageType()
        )
                : null;
        List<PassageMediaResponse> passageMedia = passageEntity != null
                ? toPassageMediaResponses(passageEntity.getPassageId())
                : List.of();

        List<AnswerAdminResponse> answers = answerRepository.findByQuestionId(questionId)
                .stream()
                .map(a -> AnswerAdminResponse.builder()
                        .answerId(a.getAnswerId())
                        .answerText(a.getAnswerText())
                        .answerLabel(a.getAnswerLabel())
                        .isCorrect(a.getIsCorrect())
                        .build()
                )
                .toList();

        return QuestionAdminResponse.builder()
                .questionId(question.getQuestionId())
                .examPartId(question.getExamPartId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .explanation(question.getExplanation())
                .examTypeId(examTypeId)
                .classId(question.getClassId())
                .isBank(question.getIsBank())
                .passage(passageDto)
                .passageMedia(passageMedia)
                .answers(answers)
                .build();
    }

    /**
     * Cập nhật câu hỏi (JSON). Đồng bộ đáp án theo {@link AnswerService#syncAnswers}.
     */
    @Transactional
    public QuestionAdminResponse updateQuestion(Long questionId, QuestionCreateRequest request, HttpServletRequest httpRequest) {
        return updateQuestion(questionId, request, httpRequest, null);
    }

    /**
     * Cập nhật câu hỏi; {@code files} (multipart) được append vào {@code passage_media} của passage liên kết.
     */
    @Transactional
    public QuestionAdminResponse updateQuestion(
            Long questionId,
            QuestionCreateRequest request,
            HttpServletRequest httpRequest,
            Map<String, MultipartFile> files
    ) {
        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng từ token.");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại."));
        if (!currentUserId.equals(question.getCreatedBy())) {
            throw new RuntimeException("Chỉ người tạo câu hỏi mới được sửa.");
        }

        if (request.getExamPartId() != null) question.setExamPartId(request.getExamPartId());
        if (request.getClassId() != null) question.setClassId(request.getClassId());
        if (request.getChapterId() != null) question.setChapterId(request.getChapterId());
        if (request.getQuestionText() != null) question.setQuestionText(request.getQuestionText());
        if (request.getQuestionType() != null) question.setQuestionType(request.getQuestionType());
        if (request.getIsBank() != null) question.setIsBank(request.getIsBank());

        Passage passage = null;
        if (request.getPassage() != null && hasPassageContent(request.getPassage())) {
            if (question.getPassageId() != null) {
                passage = passageRepository.findById(question.getPassageId()).orElse(null);
                if (passage != null) {
                    passage.setContent(request.getPassage().getContent() != null ? request.getPassage().getContent() : "");
                    passage.setPassageType(request.getPassage().getPassageType());
                    if (request.getPassage().getMediaUrl() != null) {
                        passage.setMediaUrl(request.getPassage().getMediaUrl());
                    }
                    passage = passageRepository.save(passage);
                }
            }
            if (passage == null) {
                passage = new Passage();
                passage.setContent(request.getPassage().getContent() != null ? request.getPassage().getContent() : "");
                passage.setPassageType(request.getPassage().getPassageType());
                passage.setMediaUrl(request.getPassage().getMediaUrl());
                passage = passageRepository.save(passage);
                question.setPassageId(passage.getPassageId());
            }
        } else {
            if (question.getPassageId() != null) {
                passage = passageRepository.findById(question.getPassageId()).orElse(null);
            }
        }

        boolean hasNewFiles = files != null
                && files.values().stream().anyMatch(f -> f != null && !f.isEmpty());
        if (hasNewFiles) {
            if (passage == null) {
                Passage newPassage = new Passage();
                newPassage.setContent("");
                Passage.PassageType type = Passage.PassageType.READING;
                if (request.getPassage() != null && request.getPassage().getPassageType() != null) {
                    type = request.getPassage().getPassageType();
                } else {
                    for (MultipartFile f : files.values()) {
                        if (f != null && !f.isEmpty() && f.getContentType() != null
                                && f.getContentType().startsWith("audio")) {
                            type = Passage.PassageType.LISTENING;
                            break;
                        }
                    }
                }
                newPassage.setPassageType(type);
                passage = passageRepository.save(newPassage);
                question.setPassageId(passage.getPassageId());
            }
            try {
                appendUploadedFilesToPassage(passage.getPassageId(), files.values());
            } catch (IOException e) {
                throw new RuntimeException("Upload file thất bại: " + e.getMessage(), e);
            }
            passage = passageRepository.findById(passage.getPassageId()).orElse(passage);
        }

        question = questionRepository.save(question);
        List<Answer> updatedAnswers = answerService.syncAnswers(questionId, request.getAnswers());

        return buildQuestionAdminResponse(question, passage, updatedAnswers);
    }



    @Transactional
    public List<QuestionAdminResponse> createBulkGroups(
            BulkPassageGroupRequest request,
            HttpServletRequest httpRequest,
            Map<String, MultipartFile> files
    ) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);

        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng.");
        }

        System.out.println("========== DEBUG START ==========");
        if (files != null) {
            System.out.println("Tổng số lượng file nhận được từ Controller: " + files.size());
            System.out.println("Danh sách các Key file: " + files.keySet());
        } else {
            System.out.println("Biến 'files' bị NULL!");
        }

        List<QuestionAdminResponse> allResponses = new ArrayList<>();

        for (int gIndex = 0; gIndex < request.getGroups().size(); gIndex++) {
            final int finalGIndex = gIndex;

            PassageQuestionGroup group = request.getGroups().get(gIndex);
            PassageRequest pReq = group.getPassage();

            // 🔹 SAVE PASSAGE
            Passage passage = new Passage();
            passage.setContent(pReq.getContent() != null ? pReq.getContent() : "");
            passage.setPassageType(pReq.getPassageType());

            passage = passageRepository.save(passage);

            // 🔹 MULTI MEDIA
            if (files != null) {

                List<MultipartFile> mediaFiles = files.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("media_" + finalGIndex + "_"))                        .map(Map.Entry::getValue)
                        .toList();

                for (MultipartFile file : mediaFiles) {

                    if (file == null || file.isEmpty()) continue;

                    String uploadedUrl;
                    PassageMedia.MediaType mediaType;

                    if (pReq.getPassageType() == Passage.PassageType.LISTENING) {
                        uploadedUrl = cloudinaryService.uploadAudio(file);
                        mediaType = PassageMedia.MediaType.AUDIO;
                    } else {
                        uploadedUrl = cloudinaryService.uploadImage(file);
                        mediaType = PassageMedia.MediaType.IMAGE;
                    }

                    PassageMedia media = new PassageMedia();
                    media.setPassageId(passage.getPassageId());
                    media.setMediaUrl(uploadedUrl);
                    media.setMediaType(mediaType);

                    passageMediaRepository.save(media);
                }
            }

            // 🔹 SAVE QUESTIONS
            for (NormalQuestionRequest qReq : group.getQuestions()) {

                Question question = new Question();
                question.setExamPartId(request.getExamPartId());
                question.setPassageId(passage.getPassageId());
                question.setQuestionText(qReq.getQuestionText());
                question.setQuestionType(qReq.getQuestionType());
                question.setCreatedBy(currentUserId);
                question.setIsBank(true);

                if (request.getClassId() != null)
                    question.setClassId(request.getClassId());

                if (request.getChapterId() != null)
                    question.setChapterId(request.getChapterId());

                question = questionRepository.save(question);

                List<Answer> savedAnswers =
                        saveAnswersForQuestion(
                                question.getQuestionId(),
                                qReq.getAnswers(),
                                qReq.getQuestionType()
                        );

                allResponses.add(
                        buildQuestionAdminResponse(
                                question,
                                passage,
                                savedAnswers
                        )
                );
            }
        }

        return allResponses;
    }







}
