package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.PracticeQuestionRequest;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.services.LearningVoca.PracticeCheckService;
import com.example.english_exam.services.LearningVoca.PracticeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/practice-questions")
public class PracticeController {

    @Autowired
    private PracticeService practiceService;
    @Autowired
    private PracticeCheckService practiceCheckService;

    // 1. Người dùng tự tạo câu hỏi (POST thủ công)
    @PostMapping
    public ResponseEntity<PracticeQuestionResponse> createPracticeQuestion(
            @RequestBody PracticeQuestionRequest request) {
        PracticeQuestionResponse response = practiceService.createPracticeQuestion(request);
        return ResponseEntity.ok(response);
    }
    // 3. Lấy tất cả PracticeQuestions
    @GetMapping
    public ResponseEntity<List<PracticeQuestionResponse>> getAllPracticeQuestions() {
        List<PracticeQuestionResponse> questions = practiceService.getAllPracticeQuestions();
        return ResponseEntity.ok(questions);
    }

    // 4. Lấy PracticeQuestion theo ID
    @GetMapping("/{id}")
    public ResponseEntity<PracticeQuestionResponse> getPracticeQuestionById(@PathVariable Long id) {
        return practiceService.getPracticeQuestionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Generate random câu hỏi theo album
//    @GetMapping("/gentest/{albumId}")
//    public ResponseEntity<List<PracticeQuestionResponse>> generateQuestionsForAlbum(
//            @PathVariable Long albumId) {
//        List<PracticeQuestionResponse> questions = practiceService.generatePracticeQuestionsForAlbum(albumId);
//        return ResponseEntity.ok(questions);
//    }



    @GetMapping("/generate/{albumId}")
    public ResponseEntity<PracticeQuestionResponse> generateRandomQuestion(
            @PathVariable Long albumId,
            HttpServletRequest httpRequest
    ) {
        Optional<PracticeQuestionResponse> questionOpt =
                practiceService.generateOneRandomQuestion(httpRequest, albumId);

        // ✅ Nếu không có câu hỏi (user đã học hết)
        if (questionOpt.isEmpty()) {
            return ResponseEntity.noContent().build(); // trả 204 No Content
        }

        // ✅ Nếu có câu hỏi
        return ResponseEntity.ok(questionOpt.get());
    }

}

