package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.PracticeQuestionRequest;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.services.PracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-questions")
public class PracticeQuestionController {

    @Autowired
    private PracticeService practiceService;

    // POST: tạo câu hỏi mới
    @PostMapping
    public ResponseEntity<PracticeQuestionResponse> createQuestion(@RequestBody PracticeQuestionRequest request) {
        PracticeQuestionResponse response = practiceService.createPracticeQuestion(request);
        return ResponseEntity.ok(response);
    }

    // GET: lấy danh sách tất cả câu hỏi
    @GetMapping
    public ResponseEntity<List<PracticeQuestionResponse>> getAllQuestions() {
        // nếu muốn có thêm service để convert entity -> DTO thì viết thêm findAll
        return ResponseEntity.ok(practiceService.getAllPracticeQuestions());
    }

    // GET: lấy câu hỏi theo ID
    @GetMapping("/{id}")
    public ResponseEntity<PracticeQuestionResponse> getQuestionById(@PathVariable Long id) {
        return practiceService.getPracticeQuestionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
