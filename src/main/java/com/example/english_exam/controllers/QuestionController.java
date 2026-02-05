package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.BulkCreateQuestionsToBankRequest;
import com.example.english_exam.dto.request.BulkQuestionWithPassageRequest;
import com.example.english_exam.dto.request.CreateQuestionAndAttachRequest;
import com.example.english_exam.dto.request.QuestionCreateRequest;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.Question;
import com.example.english_exam.services.ExamAndTest.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@AllArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    // =================== GET ===================

    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionAdminResponse> getQuestionById(@PathVariable Long id) {
        try {
            QuestionAdminResponse response = questionService.getQuestionDetailAdmin(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @GetMapping("/by-part/{examPartId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByPart(
            @PathVariable Long examPartId,
            @RequestParam(required = false) Long classId
    ) {
        List<QuestionResponse> questions = questionService.getQuestionsByPart(examPartId, classId);
        return ResponseEntity.ok(questions);
    }


    @GetMapping("/count/by-part/{examPartId}")
    public ResponseEntity<Long> countQuestionsByPart(
            @PathVariable Long examPartId,
            @RequestParam(required = false) Long classId
    ) {
        long count = questionService.countByExamPartId(examPartId, classId);
        return ResponseEntity.ok(count);
    }


    // =================== CREATE ===================
    // Tạo câu hỏi thông thường vào kho (không passage). Gắn đề qua API riêng.

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionAdminResponse> createQuestionToBank(
            @RequestBody QuestionCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            QuestionAdminResponse response = questionService.createQuestionToBank(request, httpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Tạo nhiều câu hỏi thông thường vào kho (không passage). */
    @PostMapping(value = "/bulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QuestionAdminResponse>> createBulkQuestionsToBankNoPassage(
            @RequestBody BulkCreateQuestionsToBankRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            List<QuestionAdminResponse> responses = questionService.createBulkQuestionsToBankNoPassage(request, httpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/bulk-with-passage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<QuestionAdminResponse>> createBulkQuestionsToBank(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "audio", required = false) MultipartFile audioFile,
            HttpServletRequest httpRequest
    ) {
        try {
            BulkQuestionWithPassageRequest request = objectMapper.readValue(requestJson, BulkQuestionWithPassageRequest.class);
            List<QuestionAdminResponse> responses = questionService.createBulkQuestionsToBank(request, httpRequest, audioFile);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Tạo câu hỏi tức thì và gắn thẳng vào part của đề (isBank = false, không lưu kho). */
    @PostMapping(value = "/create-and-attach", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionAdminResponse> createQuestionAndAttachToTest(
            @RequestBody CreateQuestionAndAttachRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            QuestionAdminResponse response = questionService.createQuestionAndAttachToTest(request, httpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // =================== DELETE ===================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
