package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.EvaluationRequest;
import com.example.english_exam.dto.response.EvaluationResponse;
import com.example.english_exam.services.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@AllArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<EvaluationResponse> create(
            HttpServletRequest httpRequest,
            @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(
                evaluationService.create(httpRequest, request)
        );
    }


    // ✅ GET ALL
    @GetMapping
    public ResponseEntity<List<EvaluationResponse>> getAll() {
        return ResponseEntity.ok(evaluationService.getAll());
    }

    // ✅ GET BY USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EvaluationResponse>> getByUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(evaluationService.getByUser(userId));
    }

    // ✅ UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<EvaluationResponse> update(
            @PathVariable Long id,
            @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(evaluationService.update(id, request));
    }

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}
