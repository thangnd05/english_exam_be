package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.PracticeCheckRequest;
import com.example.english_exam.dto.response.PracticeCheckResponse;
import com.example.english_exam.services.LearningVoca.PracticeCheckService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice-checks")
public class PracticeCheckController {

    @Autowired
    private PracticeCheckService practiceCheckService;

    @PostMapping
    public ResponseEntity<PracticeCheckResponse> submitAnswer(
            @RequestBody PracticeCheckRequest request, HttpServletRequest httpRequest) {
        PracticeCheckResponse response = practiceCheckService.checkAnswer(request,httpRequest);
        return ResponseEntity.ok(response);
    }
}
