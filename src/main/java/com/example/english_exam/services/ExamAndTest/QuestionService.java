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
    public QuestionAdminResponse createQuestionWithAnswersAdmin(QuestionRequest request, HttpServletRequest httpRequest) {

        Long currentUserId = authUtils.getUserId(httpRequest);;
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng từ token!");
        }

        Passage passageContext = null;
        Long passageId = null;

        // ✅ 1. Kiểm tra và tạo Passage trước (nếu có trong request)
        if (request.getPassage() != null) {
            Passage newPassage = new Passage();
            newPassage.setContent(request.getPassage().getContent());
            newPassage.setMediaUrl(request.getPassage().getMediaUrl());
            newPassage.setPassageType(request.getPassage().getPassageType());

            passageContext = passageRepository.save(newPassage); // Lưu và giữ lại đối tượng
            passageId = passageContext.getPassageId();
        }

        // 2. Tạo Question với passageId vừa có (hoặc null)
        Question question = new Question();
        question.setExamPartId(request.getExamPartId());
        question.setPassageId(passageId); // Sử dụng ID vừa tạo hoặc null
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setCreatedBy(currentUserId); // 🆕 thêm dòng này

        // 🔹 Gắn classId nếu có (có thể null)
        if (request.getClassId() != null) {
            question.setClassId(request.getClassId());
        }
        question = questionRepository.save(question);

        // 3. Chuẩn bị và tạo Answers
        List<Answer> answerEntities = new ArrayList<>();
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            List<Answer> answersToSave = new ArrayList<>();
            switch (request.getQuestionType()) {
                case MCQ:
                    for (AnswerRequest ar : request.getAnswers()) {
                        Answer ans = new Answer();
                        ans.setQuestionId(question.getQuestionId());
                        ans.setAnswerText(ar.getAnswerText());
                        ans.setIsCorrect(ar.getIsCorrect());
                        ans.setAnswerLabel(ar.getLabel());
                        answersToSave.add(ans);
                    }
                    break;
                case FILL_BLANK:
                    AnswerRequest ar = request.getAnswers().get(0);
                    Answer ans = new Answer();
                    ans.setQuestionId(question.getQuestionId());
                    ans.setAnswerText(ar.getAnswerText());
                    ans.setIsCorrect(true);
                    ans.setAnswerLabel(ar.getLabel() != null ? ar.getLabel() : "");
                    answersToSave.add(ans);
                    break;
            }
            // ✅ Tối ưu hóa: Dùng saveAll để lưu tất cả câu trả lời trong 1 lần gọi DB
            if (!answersToSave.isEmpty()) {
                answerEntities = answerRepository.saveAll(answersToSave);
            }
        }

        // 4. Sinh explanation nếu chưa có (không thay đổi)
        if (question.getExplanation() == null || question.getExplanation().isEmpty()) {
            String explanation = geminiService.explainQuestion(question, answerEntities, passageContext);
            question.setExplanation(explanation);
            question = questionRepository.save(question);
        }

        // 5. Build PassageResponse từ passageContext đã có (nếu tồn tại)
        PassageResponse passageResponse = null;
        if (passageContext != null) {
            passageResponse = new PassageResponse(
                    passageContext.getPassageId(),
                    passageContext.getContent(),
                    passageContext.getMediaUrl(),
                    passageContext.getPassageType().name()
            );
        }

        // 6. Convert answers sang DTO (không thay đổi)
        List<AnswerAdminResponse> answerAdminResponses = answerEntities.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(), a.getAnswerText(), a.getIsCorrect(), a.getAnswerLabel()
                ))
                .collect(Collectors.toList());

        // 7. Gán vào test_part nếu có (không thay đổi)
        if (request.getTestPartId() != null) {
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(request.getTestPartId());
            tq.setQuestionId(question.getQuestionId());
            testQuestionRepository.save(tq);
        }

        // 8. Trả về DTO admin
        return new QuestionAdminResponse(
                question.getQuestionId(),
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
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
    public List<QuestionAdminResponse> createQuestionsWithPassage(CreateQuestionsWithPassageRequest request,
                                                                  MultipartFile audioFile,
                                                                  HttpServletRequest httpRequest) throws IOException {


        Long currentUserId = authUtils.getUserId(httpRequest);
        List<QuestionAdminResponse> responses = new ArrayList<>();

        // 1️⃣ Tạo Passage trước
        Passage passage = new Passage();
        passage.setContent(request.getPassage().getContent());
        passage.setPassageType(request.getPassage().getPassageType());

        if (passage.getPassageType() == Passage.PassageType.LISTENING && audioFile != null && !audioFile.isEmpty()) {
            String audioUrl = cloudinaryService.uploadAudio(audioFile);
            passage.setMediaUrl(audioUrl);
        } else {
            passage.setMediaUrl(request.getPassage().getMediaUrl());
        }

        passage = passageRepository.save(passage);

        // 2️⃣ Tạo các Question cùng Passage
        for (NormalQuestionRequest qReq : request.getQuestions()) {
            Question question = new Question();
            question.setExamPartId(request.getExamPartId());
            question.setPassageId(passage.getPassageId());
            question.setQuestionText(qReq.getQuestionText());
            question.setQuestionType(qReq.getQuestionType());
            question.setCreatedBy(currentUserId);

            if (request.getClassId() != null) {
                question.setClassId(request.getClassId());
            }
            question = questionRepository.save(question);

            // 3️⃣ Lưu đáp án
            List<Answer> answers = new ArrayList<>();
            if (qReq.getAnswers() != null) {
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

            // 4️⃣ Build Response
            List<AnswerAdminResponse> answerDtos = answers.stream()
                    .map(a -> new AnswerAdminResponse(
                            a.getAnswerId(),
                            a.getAnswerText(),
                            a.getIsCorrect(),
                            a.getAnswerLabel()))
                    .toList();

            responses.add(new QuestionAdminResponse(
                    question.getQuestionId(),
                    question.getExamPartId(),
                    question.getQuestionText(),
                    question.getQuestionType(),
                    question.getExplanation(),
                    null,
                    answerDtos,
                    question.getClassId()
            ));
        }

        return responses;
    }

}
