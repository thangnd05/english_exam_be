package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.request.AnswerRequest;
import com.example.english_exam.dto.response.*;
import com.example.english_exam.dto.request.QuestionRequest;
import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.models.Answer;
import com.example.english_exam.models.Passage;
import com.example.english_exam.models.Question;
import com.example.english_exam.models.TestQuestion;
import com.example.english_exam.repositories.AnswerRepository;
import com.example.english_exam.repositories.PassageRepository;
import com.example.english_exam.repositories.QuestionRepository;
import com.example.english_exam.repositories.TestQuestionRepository;
import com.example.english_exam.services.ApiExtend.GeminiService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final GeminiService geminiService;
    private final TestQuestionRepository testQuestionRepository;
    private final PassageRepository  passageRepository;

    public QuestionService(QuestionRepository questionRepository, AnswerRepository answerRepository, GeminiService geminiService, TestQuestionRepository testQuestionRepository, PassageRepository passageRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.geminiService = geminiService;
        this.testQuestionRepository = testQuestionRepository;
        this.passageRepository = passageRepository;
    }

    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> findByExamPart(Long examPartId) {
        return questionRepository.findByExamPartId(examPartId);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    public QuestionAdminResponse createQuestionWithAnswersAdmin(QuestionRequest request) {

        // 1. Tạo question
        Question question = new Question();
        question.setExamPartId(request.getExamPartId());
        question.setPassageId(request.getPassageId());
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question = questionRepository.save(question);

        List<Answer> answerEntities = new ArrayList<>();

        // 2. Tạo answers dựa trên questionType
        switch (question.getQuestionType()) {
            case MCQ:
                for (AnswerRequest ar : request.getAnswers()) {
                    Answer ans = new Answer();
                    ans.setQuestionId(question.getQuestionId());
                    ans.setAnswerText(ar.getAnswerText());
                    ans.setIsCorrect(ar.getIsCorrect());
                    ans.setAnswerLabel(ar.getLabel());
                    answerEntities.add(answerRepository.save(ans));
                }
                break;

            case FILL_BLANK:
                if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
                    AnswerRequest ar = request.getAnswers().get(0);
                    Answer ans = new Answer();
                    ans.setQuestionId(question.getQuestionId());
                    ans.setAnswerText(ar.getAnswerText());
                    ans.setIsCorrect(true);
                    ans.setAnswerLabel(ar.getLabel());
                    answerEntities.add(answerRepository.save(ans));
                }
                break;

            case ESSAY:
                // ESSAY: không tạo answer
                break;

            default:
                throw new IllegalArgumentException("Unknown question type: " + question.getQuestionType());
        }

        // 3. Truy vấn passage nếu có
        Passage passageContext = null;
        PassageResponse passageResponse = null;
        if (question.getPassageId() != null) {
            passageContext = passageRepository.findById(question.getPassageId()).orElse(null);
            if (passageContext != null) {
                passageResponse = new PassageResponse(
                        passageContext.getPassageId(),
                        passageContext.getContent(),
                        passageContext.getMediaUrl(),
                        passageContext.getPassageType().name()
                );
            }
        }

        // 4. Sinh explanation nếu chưa có
        if (question.getExplanation() == null || question.getExplanation().isEmpty()) {
            String explanation = geminiService.explainQuestion(question, answerEntities, passageContext);
            question.setExplanation(explanation);
            question = questionRepository.save(question);
        }

        // 5. Convert answers sang DTO (có isCorrect)
        List<AnswerAdminResponse> answerAdminResponses = answerEntities.stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),   // có isCorrect
                        a.getAnswerLabel()
                ))
                .collect(Collectors.toList());


        // 6. Gán vào test_part nếu có
        if (request.getTestPartId() != null) {
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(request.getTestPartId());
            tq.setQuestionId(question.getQuestionId());
            testQuestionRepository.save(tq);
        }

        // 7. Trả về DTO admin
        return new QuestionAdminResponse(
                question.getQuestionId(),
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
                request.getTestPartId(),
                passageResponse,
                answerAdminResponses
        );
    }


    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }
}
