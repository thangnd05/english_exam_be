package com.example.english_exam.services;

import com.example.english_exam.dto.request.AnswerRequest;
import com.example.english_exam.dto.response.AnswerResponse;
import com.example.english_exam.dto.request.QuestionRequest;
import com.example.english_exam.dto.response.QuestionResponse;
import com.example.english_exam.models.Answer;
import com.example.english_exam.models.Question;
import com.example.english_exam.models.TestQuestion;
import com.example.english_exam.repositories.AnswerRepository;
import com.example.english_exam.repositories.QuestionRepository;
import com.example.english_exam.repositories.TestQuestionRepository;
import com.example.english_exam.services.ApiExtend.GeminiService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final GeminiService geminiService;
    private final TestQuestionRepository testQuestionRepository;

    public QuestionService(QuestionRepository questionRepository, AnswerRepository answerRepository, GeminiService geminiService, TestQuestionRepository testQuestionRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.geminiService = geminiService;
        this.testQuestionRepository = testQuestionRepository;
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

    public QuestionResponse createQuestionWithAnswers(QuestionRequest request) {
        // 1. Tạo question
        Question question = new Question();
        question.setExamPartId(request.getExamPartId());
        question.setPassageId(request.getPassageId());
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question = questionRepository.save(question);

        // 2. Tạo answers
        List<Answer> answerEntities = new ArrayList<>();
        for (AnswerRequest ar : request.getAnswers()) {
            Answer ans = new Answer();
            ans.setQuestionId(question.getQuestionId());
            ans.setAnswerText(ar.getAnswerText());
            ans.setIsCorrect(ar.getIsCorrect());
            ans.setAnswerLabel(ar.getLabel());
            answerEntities.add(answerRepository.save(ans));
        }

        // 3. Sinh explanation nếu chưa có
        if (question.getExplanation() == null || question.getExplanation().isEmpty()) {
            String explanation = geminiService.explainQuestion(question, answerEntities);
            question.setExplanation(explanation);
            question = questionRepository.save(question);
        }

        // 4. Chuyển answers sang DTO response
        List<AnswerResponse> answerResponses = new ArrayList<>();
        for (Answer a : answerEntities) {
            AnswerResponse ar = new AnswerResponse();
            ar.setAnswerId(a.getAnswerId());
            ar.setAnswerText(a.getAnswerText());
            ar.setIsCorrect(a.getIsCorrect());
            ar.setAnswerLabel(a.getAnswerLabel());
            answerResponses.add(ar);
        }
        // 5. Gán câu hỏi vào test_questions (nếu có test_part_id truyền vào)
        if (request.getTestPartId() != null) {
            TestQuestion tq = new TestQuestion();
            tq.setTestPartId(request.getTestPartId());
            tq.setQuestionId(question.getQuestionId());
            testQuestionRepository.save(tq);
        }

        // 6. Trả về QuestionResponse
        return new QuestionResponse(
                question.getQuestionId(),
                question.getExamPartId(),
                question.getPassageId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
                answerResponses,
                request.getTestPartId() // gán thêm testPartId vào response
        );



    }

    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }
}
