package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.request.PartRequest;
import com.example.english_exam.dto.request.TestRequest;
import com.example.english_exam.dto.response.AnswerResponse;
import com.example.english_exam.dto.response.QuestionResponse;
import com.example.english_exam.dto.response.TestPartResponse;
import com.example.english_exam.dto.response.TestResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TestService {
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestPartRepository testPartRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AnswerService answerService;

    public TestService(TestRepository testRepository, QuestionRepository questionRepository, TestPartRepository testPartRepository, TestQuestionRepository testQuestionRepository, AnswerService answerService) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testPartRepository = testPartRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.answerService = answerService;
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

    public TestResponse createTest(TestRequest request) {
        // 1. Tạo Test
        Test test = new Test();
        test.setTitle(request.getTitle());
        test.setDescription(request.getDescription());
        test.setExamTypeId(request.getExamTypeId());
        test.setCreatedBy(request.getStudentId());
        test.setCreatedAt(LocalDateTime.now());
        test = testRepository.save(test);

        List<TestPartResponse> partResponses = new ArrayList<>();

        // 2. Với mỗi part → random câu hỏi
        for (PartRequest partReq : request.getParts()) {
            // 2a. Tạo TestPart
            TestPart testPart = new TestPart();
            testPart.setTestId(test.getTestId());
            testPart.setExamPartId(partReq.getExamPartId());
            testPart.setNumQuestions(partReq.getNumQuestions());
            testPart = testPartRepository.save(testPart);

            // 2b. Lấy hoặc random câu hỏi
            List<Question> questions = questionRepository.findRandomByExamPart(
                    partReq.getExamPartId(), partReq.getNumQuestions());

            List<QuestionResponse> questionResponses = new ArrayList<>();

            for (Question q : questions) {
                // 2c. Chỉ tạo TestQuestion nếu chưa gắn vào testPart
                boolean alreadyLinked = testQuestionRepository.existsByQuestionIdAndTestPartId(q.getQuestionId(), testPart.getTestPartId());
                if (!alreadyLinked) {
                    TestQuestion tq = new TestQuestion();
                    tq.setTestPartId(testPart.getTestPartId());
                    tq.setQuestionId(q.getQuestionId());
                    testQuestionRepository.save(tq);
                }

                // 2d. Lấy đáp án
                List<AnswerResponse> answerResponses = answerService.getAnswersByQuestionId(q.getQuestionId());

                // 2e. Tạo QuestionResponse
                questionResponses.add(new QuestionResponse(
                        q.getQuestionId(),
                        q.getExamPartId(),
                        q.getPassageId(),
                        q.getQuestionText(),
                        q.getQuestionType(),
                        q.getExplanation(),
                        answerResponses,
                        testPart.getTestPartId()
                ));
            }

            // 2f. Tạo TestPartResponse
            partResponses.add(new TestPartResponse(
                    testPart.getTestPartId(),
                    testPart.getExamPartId(),
                    testPart.getNumQuestions(),
                    questionResponses
            ));
        }

        // 3. Trả về TestResponse
        return new TestResponse(
                test.getTestId(),
                test.getTitle(),
                test.getDescription(),
                test.getExamTypeId(),
                test.getCreatedBy(),
                test.getCreatedAt(),
                partResponses
        );
    }

}
