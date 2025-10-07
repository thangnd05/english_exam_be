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
import com.example.english_exam.services.ApiExtend.GeminiService;
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
    public QuestionAdminResponse createQuestionWithAnswersAdmin(QuestionRequest request) {

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
                answerAdminResponses
        );
    }

    public List<QuestionResponse> getQuestionsByPart(Long examPartId) {
        List<Question> questions = questionRepository.findByExamPartId(examPartId);

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

    @Transactional // ✅ Bọc toàn bộ phương thức trong một giao dịch để đảm bảo an toàn
    public NormalQuestionAdminResponse createNormalQuestion(NormalQuestionRequest request) {

        Long passageId = null;
        Passage passageContext = null;

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
        ExamPart examPart = examPartRepository.findByName("Default");
        Question question = new Question();
        question.setExamPartId(examPart.getExamPartId());
        question.setPassageId(passageId); // Sử dụng ID vừa tạo hoặc null
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question = questionRepository.save(question);

        // 3. Tạo Answers (đã tối ưu hóa)
        List<Answer> answerEntities = new ArrayList<>();
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            List<Answer> answersToSave = new ArrayList<>();
            switch (question.getQuestionType()) {
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
                case ESSAY:
                    // không cần answer
                    break;
            }
            // ✅ Tối ưu hóa: Dùng saveAll để lưu tất cả trong 1 lần gọi DB
            if (!answersToSave.isEmpty()) {
                answerEntities = answerRepository.saveAll(answersToSave);
            }
        }

        // 4. Build PassageResponse từ đối tượng đã có, không cần query lại
        PassageResponse passageResponse = null;
        if (passageContext != null) {
            passageResponse = new PassageResponse(
                    passageContext.getPassageId(),
                    passageContext.getContent(),
                    passageContext.getMediaUrl(),
                    passageContext.getPassageType().name()
            );
        }

        // 5. Convert answers sang DTO
        List<AnswerAdminResponse> answerResponses = answerEntities.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(), a.getAnswerText(), a.getIsCorrect(), a.getAnswerLabel()
                ))
                .collect(Collectors.toList());

        // 6. Trả về DTO
        return new NormalQuestionAdminResponse(
                question.getQuestionId(),
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                passageResponse,
                answerResponses
        );
    }

    public long countByExamPartId(Long examPartId) {
        return questionRepository.countByExamPartId(examPartId);
    }


    @Transactional
    public List<QuestionAdminResponse> createQuestionsWithPassage(
            CreateQuestionsWithPassageRequest request,
            MultipartFile audioFile
    ) throws IOException {

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
                    answerDtos
            ));
        }

        return responses;
    }

}
