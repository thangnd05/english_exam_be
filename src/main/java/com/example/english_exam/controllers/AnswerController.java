package com.example.english_exam.controllers;

import com.example.english_exam.models.Answer;
import com.example.english_exam.services.AnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {
    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }
    @GetMapping
    public ResponseEntity<List<Answer>> getAllAnswer() {
        return ResponseEntity.ok(answerService.findAll());
    }

    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<List<Answer>> getAnswersByQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(answerService.findByQuestionId(questionId));
    }

    @PostMapping
    public ResponseEntity<Answer> createAnswer(@RequestBody Answer answer) {
        return ResponseEntity.ok(answerService.save(answer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Answer> updateAnswer(@PathVariable Long id, @RequestBody Answer updatedAnswer) {
        return answerService.findById(id)
                .map(existing -> {
                    updatedAnswer.setAnswerId(id);
                    return ResponseEntity.ok(answerService.save(updatedAnswer));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnswer(@PathVariable Long id) {
        return answerService.findById(id)
                .map(existing -> {
                    answerService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
