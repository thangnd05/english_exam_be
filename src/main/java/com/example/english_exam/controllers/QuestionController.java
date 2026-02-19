package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.*;
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
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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


    /** Cá nhân: không gửi classId/chapterId (lấy theo user JWT). Lớp: gửi classId (+ chapterId). */
    @GetMapping("/by-part/{examPartId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByPart(
            @PathVariable Long examPartId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long chapterId,
            HttpServletRequest request
    ) {
        List<QuestionResponse> questions = questionService.getQuestionsByPart(examPartId, classId, chapterId, request);
        return ResponseEntity.ok(questions);
    }

    /** Cá nhân: không gửi classId/chapterId. Lớp: gửi classId (+ chapterId). */
    @GetMapping("/count/by-part/{examPartId}")
    public ResponseEntity<Long> countQuestionsByPart(
            @PathVariable Long examPartId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long chapterId,
            HttpServletRequest request
    ) {
        long count = questionService.countByExamPartId(examPartId, classId, chapterId, request);
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


    @PostMapping(
            value = "/bulk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<QuestionAdminResponse>> createBulkQuestionsToBankNoPassage(
            @RequestPart("request") String requestJson,
            HttpServletRequest httpRequest
    ) {

        try {

            // 1️⃣ Parse JSON
            BulkCreateQuestionsToBankRequest request =
                    objectMapper.readValue(
                            requestJson,
                            BulkCreateQuestionsToBankRequest.class
                    );

            // 2️⃣ Lấy toàn bộ file
            MultipartHttpServletRequest multipartRequest =
                    (MultipartHttpServletRequest) httpRequest;

            Map<String, MultipartFile> files =
                    multipartRequest.getFileMap();

            // 3️⃣ Gọi service
            List<QuestionAdminResponse> responses =
                    questionService.createBulkQuestionsToBankNoPassage(
                            request,
                            httpRequest,
                            files
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(responses);

        } catch (Exception e) {
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

    @PostMapping(value = "/create-and-attach", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuestionAdminResponse> createQuestionAndAttachToTest(
            @RequestPart("request") String requestJson,
            HttpServletRequest httpRequest
    ) throws IOException {

        // Lấy toàn bộ file từ request
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) httpRequest;
        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        CreateQuestionAndAttachRequest request =
                objectMapper.readValue(requestJson, CreateQuestionAndAttachRequest.class);

        QuestionAdminResponse result =
                questionService.createQuestionAndAttachToTest(request, httpRequest, files);

        return ResponseEntity.ok(result);
    }

    // =================== DELETE ===================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping(
            value = "/bulk-groups",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<QuestionAdminResponse>> createBulkGroups(
            @RequestPart("request") String requestJson,
            HttpServletRequest httpRequest
    ) throws IOException {

        // 1. Ép kiểu request sang MultipartRequest
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) httpRequest;

        // 2. Lấy toàn bộ Map chứa các file (key là tên media_x_x, value là file)
        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        // 3. Parse JSON thủ công
        BulkPassageGroupRequest request =
                objectMapper.readValue(requestJson, BulkPassageGroupRequest.class);

        // 4. Gọi service
        List<QuestionAdminResponse> result =
                questionService.createBulkGroups(request, httpRequest, files);

        return ResponseEntity.ok(result);
    }

}
