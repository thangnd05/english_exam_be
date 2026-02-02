package com.example.english_exam.services;

import com.example.english_exam.dto.request.EvaluationRequest;
import com.example.english_exam.dto.response.EvaluationResponse;
import com.example.english_exam.models.Evaluation;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.EvaluationRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;


    // ============================
    // ✅ Helper convert Entity → DTO
    // ============================
    private EvaluationResponse toResponse(Evaluation e) {

        User user = userRepository.findById(e.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new EvaluationResponse(
                e.getId(),
                e.getContent(),
                e.getRating(),
                e.getCreatedAt(),

                user.getUserId(),
                user.getUserName(),
                user.getAvatarUrl()
        );
    }

    // ============================
    // ✅ CREATE
    // ============================
    public EvaluationResponse create(HttpServletRequest httpRequest, EvaluationRequest request) {

        Long currentUserId = authUtils.getUserId(httpRequest);

        Evaluation evaluation = new Evaluation();
        evaluation.setUserId(currentUserId);
        evaluation.setContent(request.getContent());
        evaluation.setRating(request.getRating());

        Evaluation saved = evaluationRepository.save(evaluation);

        return toResponse(saved);
    }

    // ============================
    // ✅ GET ALL
    // ============================
    public List<EvaluationResponse> getAll() {
        return evaluationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================
    // ✅ GET BY USER
    // ============================
    public List<EvaluationResponse> getByUser(Long userId) {
        return evaluationRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================
    // ✅ UPDATE
    // ============================
    public EvaluationResponse update(Long id, EvaluationRequest request) {

        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));

        evaluation.setContent(request.getContent());
        evaluation.setRating(request.getRating());

        return toResponse(evaluationRepository.save(evaluation));
    }

    // ============================
    // ✅ DELETE
    // ============================
    public void delete(Long id) {
        evaluationRepository.deleteById(id);
    }
}
