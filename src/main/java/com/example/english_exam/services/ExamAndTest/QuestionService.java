package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.*;
import com.example.english_exam.dto.response.*;
import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.admin.NormalQuestionAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import com.example.english_exam.security.AuthService;
import com.example.english_exam.services.ApiExtend.GeminiService;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final GeminiService geminiService;
    private final TestQuestionRepository testQuestionRepository;
    private final PassageRepository  passageRepository;
    private final ExamPartRepository examPartRepository;
    private final CloudinaryService  cloudinaryService;
    private final AuthUtils authUtils;



    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    @Transactional // ✅ Bọc toàn bộ phương thức trong một giao dịch
    public QuestionAdminResponse createQuestionWithAnswersAdmin(
            QuestionRequest request,
            HttpServletRequest httpRequest) {

        Long currentUserId = authUtils.getUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng từ token!");
        }

        // ✅ 1. Kiểm tra và tạo Passage trước (nếu có trong request)
        Passage passageContext = null;
        Long passageId = null;

        if (request.getPassage() != null &&
                ((request.getPassage().getContent() != null && !request.getPassage().getContent().trim().isEmpty()) ||
                        request.getPassage().getMediaUrl() != null)) {

            Passage newPassage = new Passage();
            newPassage.setContent(request.getPassage().getContent());
            newPassage.setMediaUrl(request.getPassage().getMediaUrl());
            newPassage.setPassageType(request.getPassage().getPassageType());

            passageContext = passageRepository.save(newPassage);
            passageId = passageContext.getPassageId();
        }


        // ✅ 2. Tạo Question với passageId vừa có (hoặc null)
        Question question = new Question();
        question.setExamPartId(request.getExamPartId());
        question.setPassageId(passageId);
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setCreatedBy(currentUserId);

        if (request.getClassId() != null) {
            question.setClassId(request.getClassId());
        }

        question = questionRepository.save(question);

        // ✅ 3. Tạo danh sách Answer
        List<Answer> answerEntities = new ArrayList<>();

        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            List<Answer> answersToSave = new ArrayList<>();

            switch (request.getQuestionType()) {
                case MCQ -> {
                    for (AnswerRequest ar : request.getAnswers()) {
                        Answer ans = new Answer();
                        ans.setQuestionId(question.getQuestionId());
                        ans.setAnswerText(ar.getAnswerText());
                        ans.setIsCorrect(ar.getIsCorrect());
                        ans.setAnswerLabel(ar.getLabel());
                        answersToSave.add(ans);
                    }
                }
                case FILL_BLANK -> {
                    AnswerRequest ar = request.getAnswers().get(0);
                    Answer ans = new Answer();
                    ans.setQuestionId(question.getQuestionId());
                    ans.setAnswerText(ar.getAnswerText());
                    ans.setIsCorrect(true);
                    ans.setAnswerLabel(ar.getLabel() != null ? ar.getLabel() : "");
                    answersToSave.add(ans);
                }
            }

            if (!answersToSave.isEmpty()) {
                answerEntities = answerRepository.saveAll(answersToSave);
            }
        }

        // ✅ 4. Sinh explanation nếu chưa có
        if (question.getExplanation() == null || question.getExplanation().isEmpty()) {
            String explanation = geminiService.explainQuestion(question, answerEntities, passageContext);
            question.setExplanation(explanation);
            question = questionRepository.save(question);
        }

        // ✅ 5. Build PassageResponse nếu có
        PassageResponse passageResponse = null;
        if (passageContext != null) {
            passageResponse = new PassageResponse(
                    passageContext.getPassageId(),
                    passageContext.getContent(),        // đúng với DTO
                    passageContext.getMediaUrl(),
                    passageContext.getPassageType()
            );

        }

        // ✅ 6. Convert answers sang DTO
        List<AnswerAdminResponse> answerAdminResponses = answerEntities.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),
                        a.getAnswerLabel()
                ))
                .toList();

        // ✅ 7. Gán vào test_part nếu có
        if (request.getTestPartId() != null) {
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(request.getTestPartId());
            tq.setQuestionId(question.getQuestionId());
            testQuestionRepository.save(tq);
        }

        // ✅ 8. Lấy examTypeId từ examPart để trả ra FE
        Long examTypeId = examPartRepository.findById(question.getExamPartId())
                .map(p -> p.getExamTypeId())
                .orElse(null);

        // ✅ 9. Trả về DTO đầy đủ cho FE
        return new QuestionAdminResponse(
                question.getQuestionId(),
                examTypeId,                           // 🟢 mới thêm
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
                passageResponse,                      // 🟢 mới thêm
                request.getTestPartId(),
                answerAdminResponses,
                question.getClassId()
        );
    }

    public List<QuestionResponse> getQuestionsByPart(Long examPartId, Long classId) {
        List<Question> questions;

        // 🟢 Nếu có classId thì chỉ lấy câu hỏi trong lớp đó
        if (classId != null) {
            questions = questionRepository.findByExamPartIdAndClassId(examPartId, classId);
        } else {
            questions = questionRepository.findByExamPartId(examPartId);
        }

        List<QuestionResponse> responses = new ArrayList<>();
        for (Question q : questions) {
            QuestionResponse dto = new QuestionResponse();
            dto.setQuestionId(q.getQuestionId());
            dto.setExamPartId(q.getExamPartId());
            dto.setQuestionText(q.getQuestionText());
            dto.setQuestionType(q.getQuestionType());
            dto.setExplanation(q.getExplanation());

            List<Answer> answers = answerRepository.findByQuestionId(q.getQuestionId());
            List<AnswerResponse> answerDtos = answers.stream()
                    .map(a -> new AnswerResponse(a.getAnswerId(), a.getAnswerText(), a.getAnswerLabel()))
                    .collect(Collectors.toList());

            dto.setAnswers(answerDtos);
            responses.add(dto);
        }

        return responses;
    }



    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }

    public long countByExamPartId(Long examPartId, Long classId) {
        if (classId != null) {
            // 🟢 Đếm theo lớp nếu có
            return questionRepository.countByExamPartIdAndClassId(examPartId, classId);
        } else {
            // 🟢 Không có lớp → đếm toàn bộ
            return questionRepository.countByExamPartId(examPartId);
        }
    }



    @Transactional
    public List<QuestionAdminResponse> createQuestionsWithPassage(
            CreateQuestionsWithPassageRequest request,
            MultipartFile audioFile,
            HttpServletRequest httpRequest) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);
        List<QuestionAdminResponse> responses = new ArrayList<>();

        // 🟢 1️⃣ Kiểm tra xem có passage không
        Passage passage = null;
        if (request.getPassage() != null &&
                ((request.getPassage().getContent() != null && !request.getPassage().getContent().trim().isEmpty()) ||
                        (request.getPassage().getPassageType() == Passage.PassageType.LISTENING &&
                                audioFile != null && !audioFile.isEmpty()))) {

            passage = new Passage();
            passage.setContent(request.getPassage().getContent());
            passage.setPassageType(request.getPassage().getPassageType());

            if (passage.getPassageType() == Passage.PassageType.LISTENING
                    && audioFile != null && !audioFile.isEmpty()) {
                String audioUrl = cloudinaryService.uploadAudio(audioFile);
                passage.setMediaUrl(audioUrl);
            } else {
                passage.setMediaUrl(request.getPassage().getMediaUrl());
            }

            passage = passageRepository.save(passage);
        }

        // 🟢 2️⃣ Lấy examTypeId qua examPartId
        Long examTypeId = examPartRepository.findById(request.getExamPartId())
                .map(p -> p.getExamTypeId())
                .orElse(null);

        // 🟢 3️⃣ Chuẩn bị PassageResponse (nếu có)
        PassageResponse passageResponse = (passage != null)
                ? new PassageResponse(
                passage.getPassageId(),
                passage.getContent(),
                passage.getMediaUrl(),
                passage.getPassageType())
                : null;

        // 🟢 4️⃣ Tạo các Question
        for (NormalQuestionRequest qReq : request.getQuestions()) {
            Question question = new Question();
            question.setExamPartId(request.getExamPartId());
            if (passage != null) question.setPassageId(passage.getPassageId());
            question.setQuestionText(qReq.getQuestionText());
            question.setQuestionType(qReq.getQuestionType());
            question.setCreatedBy(currentUserId);

            if (request.getClassId() != null) {
                question.setClassId(request.getClassId());
            }

            question = questionRepository.save(question);

            // 🟢 5️⃣ Lưu đáp án
            List<Answer> answers = new ArrayList<>();
            if (qReq.getAnswers() != null && !qReq.getAnswers().isEmpty()) {
                for (AnswerRequest aReq : qReq.getAnswers()) {
                    Answer ans = new Answer();
                    ans.setQuestionId(question.getQuestionId());
                    ans.setAnswerText(aReq.getAnswerText());
                    ans.setAnswerLabel(aReq.getLabel());
                    ans.setIsCorrect(aReq.getIsCorrect());
                    answers.add(ans);
                }
                answerRepository.saveAll(answers);
            }

            // 🟢 6️⃣ Convert sang DTO
            List<AnswerAdminResponse> answerDtos = answers.stream()
                    .map(a -> new AnswerAdminResponse(
                            a.getAnswerId(),
                            a.getAnswerText(),
                            a.getIsCorrect(),
                            a.getAnswerLabel()))
                    .toList();

            // 🟢 7️⃣ Tạo QuestionAdminResponse đầy đủ
            QuestionAdminResponse response = new QuestionAdminResponse(
                    question.getQuestionId(),
                    examTypeId,
                    question.getExamPartId(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getExplanation(),
                    passageResponse, // null nếu không có passage
                    null,
                    answerDtos,
                    question.getClassId()
            );

            responses.add(response);
        }

        return responses;
    }



    @Transactional
    public QuestionAdminResponse updateQuestionWithPassage(
            Long questionId,
            QuestionRequest request,
            MultipartFile audioFile,
            HttpServletRequest httpRequest) throws IOException {

        Long currentUserId = authUtils.getUserId(httpRequest);

        // 🔹 Tìm câu hỏi cũ
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // 🔹 Kiểm tra quyền sửa
        if (!existing.getCreatedBy().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền sửa câu hỏi này!");
        }

        // 🔹 Cập nhật nội dung question
        existing.setQuestionText(request.getQuestionText());
        existing.setQuestionType(request.getQuestionType());
        if (request.getClassId() != null) {
            existing.setClassId(request.getClassId());
        }

        // 🔹 Cập nhật passage (nếu có)
        Passage passage = null;
        if (request.getPassage() != null) {
            if (existing.getPassageId() != null) {
                passage = passageRepository.findById(existing.getPassageId())
                        .orElse(new Passage());
            } else {
                passage = new Passage();
            }

            passage.setContent(request.getPassage().getContent());
            passage.setPassageType(request.getPassage().getPassageType());

            // 🔹 Nếu là Listening và có audio mới → upload lại
            if (passage.getPassageType() == Passage.PassageType.LISTENING
                    && audioFile != null && !audioFile.isEmpty()) {
                String audioUrl = cloudinaryService.uploadAudio(audioFile);
                passage.setMediaUrl(audioUrl);
            } else {
                passage.setMediaUrl(request.getPassage().getMediaUrl());
            }

            passage = passageRepository.save(passage);
            existing.setPassageId(passage.getPassageId());
        }

        questionRepository.save(existing);

        // 🔹 Xóa đáp án cũ theo questionId (chú ý deleteByQuestionId, không phải deleteById)
        answerRepository.deleteByQuestionId(existing.getQuestionId());

        // 🔹 Thêm mới danh sách đáp án
        List<Answer> answers = new ArrayList<>();
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            for (AnswerRequest aReq : request.getAnswers()) {
                Answer ans = new Answer();
                ans.setQuestionId(existing.getQuestionId());
                ans.setAnswerText(aReq.getAnswerText());
                ans.setAnswerLabel(aReq.getLabel());
                ans.setIsCorrect(aReq.getIsCorrect());
                answers.add(ans);
            }
            answerRepository.saveAll(answers);
        }

        // 🔹 Convert Answer sang DTO
        List<AnswerAdminResponse> answerDtos = answers.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),
                        a.getAnswerLabel()))
                .toList();

        // 🔹 Build PassageResponse nếu có
        PassageResponse passageDto = null;
        if (passage != null) {
            passageDto = new PassageResponse(
                    passage.getPassageId(),
                    passage.getContent(),
                    passage.getMediaUrl(),
                    passage.getPassageType()
            );
        } else if (existing.getPassageId() != null) {
            // Nếu passage không gửi lại nhưng question đã có passage
            Passage oldPassage = passageRepository.findById(existing.getPassageId()).orElse(null);
            if (oldPassage != null) {
                passageDto = new PassageResponse(
                        oldPassage.getPassageId(),
                        oldPassage.getContent(),
                        oldPassage.getMediaUrl(),
                        oldPassage.getPassageType()
                );
            }
        }

        // 🔹 Lấy examTypeId thông qua examPart
        Long examTypeId = examPartRepository.findById(existing.getExamPartId())
                .map(p -> p.getExamTypeId())
                .orElse(null);

        // 🔹 Trả về DTO đầy đủ
        return new QuestionAdminResponse(
                existing.getQuestionId(),
                examTypeId,                      // 🟢 mới thêm
                existing.getExamPartId(),
                existing.getQuestionText(),
                existing.getQuestionType(),
                existing.getExplanation(),
                passageDto,                      // 🟢 mới thêm
                null,
                answerDtos,
                existing.getClassId()
        );
    }


    public QuestionAdminResponse getQuestionDetailAdmin(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // 🔹 Lấy examTypeId qua examPart
        Long examTypeId = examPartRepository.findById(question.getExamPartId())
                .map(p -> p.getExamTypeId())
                .orElse(null);

        // 🔹 Lấy passage
        PassageResponse passageDto = null;
        if (question.getPassageId() != null) {
            Passage p = passageRepository.findById(question.getPassageId())
                    .orElse(null);
            if (p != null) {
                passageDto = new PassageResponse(
                        p.getPassageId(),
                        p.getContent(),
                        p.getMediaUrl(),
                        p.getPassageType()
                );
            }
        }

        // 🔹 Lấy danh sách đáp án
        List<AnswerAdminResponse> answers = answerRepository.findByQuestionId(questionId)
                .stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),
                        a.getAnswerLabel()
                ))
                .toList();

        // 🔹 Build DTO trả ra
        return new QuestionAdminResponse(
                question.getQuestionId(),
                examTypeId,
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
                passageDto,
                null, // testPartId
                answers,
                question.getClassId()
        );

    }




}
