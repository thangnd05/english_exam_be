package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.CreateQuestionsWithPassageRequest;
import com.example.english_exam.dto.request.NormalQuestionRequest;
import com.example.english_exam.dto.request.QuestionRequest;
import com.example.english_exam.dto.response.admin.NormalQuestionAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.Question;
import com.example.english_exam.services.ExamAndTest.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/questions")
@AllArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final ObjectMapper objectMapper; // <-- 1. Khai báo một field final



    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Optional<Question> question = questionService.findById(id);
        return question.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(
            value = "/create-with-passage",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createQuestionsWithPassage(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile
    ) {
        try {
            CreateQuestionsWithPassageRequest request =
                    objectMapper.readValue(dataJson, CreateQuestionsWithPassageRequest.class);

            List<QuestionAdminResponse> responses =
                    questionService.createQuestionsWithPassage(request, audioFile);

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error: " + e.getMessage());
        }
    }


    @PostMapping
    public ResponseEntity<QuestionAdminResponse> createQuestionWithAnswers(
            @RequestBody QuestionRequest request) {

        QuestionAdminResponse response = questionService.createQuestionWithAnswersAdmin(request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/normal")
    public ResponseEntity<NormalQuestionAdminResponse> createNormalQuestion(
            @RequestBody NormalQuestionRequest request) {

        NormalQuestionAdminResponse response = questionService.createNormalQuestion(request);
        return ResponseEntity.ok(response);
    }

    // ✅ Lấy danh sách câu hỏi theo examPartId (cho FE tạo đề)
    @GetMapping("/by-part/{examPartId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByPart(@PathVariable Long examPartId) {
        List<QuestionResponse> list = questionService.getQuestionsByPart(examPartId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/count/by-part/{examPartId}")
    public ResponseEntity<Long> countQuestionsByPart(@PathVariable Long examPartId) {
        long count = questionService.countByExamPartId(examPartId);
        return ResponseEntity.ok(count);
    }


}
