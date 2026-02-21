package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.UserAnswerRequest;
import com.example.english_exam.dto.response.ResultSummaryDto;
import com.example.english_exam.models.UserAnswer;
import com.example.english_exam.services.ExamAndTest.AnswerService;
import com.example.english_exam.services.ExamAndTest.UserAnswerService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-answers")
@AllArgsConstructor
public class UserAnswerController {

    private final UserAnswerService userAnswerService;
    private final AnswerService answerService;

    @GetMapping
    public ResponseEntity<List<UserAnswer>> getAll() {
        return ResponseEntity.ok(userAnswerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAnswer> getById(@PathVariable Long id) {
        return userAnswerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user-test/{userTestId}")
    public ResponseEntity<List<UserAnswer>> getByUserTest(@PathVariable Long userTestId) {
        return ResponseEntity.ok(userAnswerService.findByUserTestId(userTestId));
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<UserAnswer>> getByQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(userAnswerService.findByQuestionId(questionId));
    }

    @PostMapping
    public ResponseEntity<UserAnswer> create(@RequestBody UserAnswer userAnswer) {
        return ResponseEntity.ok(userAnswerService.save(userAnswer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAnswer> update(@PathVariable Long id, @RequestBody UserAnswer userAnswer) {
        userAnswer.setUserAnswerId(id);
        return ResponseEntity.ok(userAnswerService.save(userAnswer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return userAnswerService.findById(id)
                .map(existing -> {
                    userAnswerService.delete(id);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    @PostMapping("/batch")
    public ResponseEntity<?> saveUserAnswers(@RequestBody List<UserAnswerRequest> requests) {
        for (UserAnswerRequest a : requests) {
            UserAnswer ua = new UserAnswer();
            ua.setUserTestId(a.getUserTestId());
            ua.setQuestionId(a.getQuestionId());
            ua.setSelectedAnswerId(a.getSelectedAnswerId());
            ua.setAnswerText(a.getAnswerText());

            userAnswerService.save(ua);
        }

        return ResponseEntity.ok("Saved all answers!");
    }

    @GetMapping("/user-test/{userTestId}/result")
    public ResponseEntity<ResultSummaryDto> getResult(@PathVariable Long userTestId) {
        // ✅ Chỉ cần gọi phương thức service và trả về kết quả
        ResultSummaryDto result = userAnswerService.getResultSummary(userTestId);
        return ResponseEntity.ok(result);
    }


}
