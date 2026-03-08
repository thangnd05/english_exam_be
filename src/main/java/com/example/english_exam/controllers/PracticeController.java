package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.PracticeCheckRequest;
import com.example.english_exam.dto.response.PracticeCheckResponse;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.services.LearningVoca.PracticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice-questions")
@AllArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/generate/{albumId}")
    public ResponseEntity<PracticeQuestionResponse> generate(HttpServletRequest request, @PathVariable Long albumId) {
        return practiceService.generateOneRandomQuestion(request, albumId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/mark-known/{vocabId}")
    public ResponseEntity<String> markWordKnown(@PathVariable Long vocabId, HttpServletRequest httpRequest) {
        practiceService.markWordAsKnown(httpRequest, vocabId);
        return ResponseEntity.ok("Đã đánh dấu từ này là đã biết");
    }

    @PostMapping("/check")
    public ResponseEntity<PracticeCheckResponse> check(HttpServletRequest request, @RequestBody PracticeCheckRequest req) {
        return ResponseEntity.ok(practiceService.checkAnswer(request, req));
    }
}

