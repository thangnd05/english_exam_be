package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.dto.request.UserVocabularyRequest;
import com.example.english_exam.dto.response.UserVocabularyResponse;
import com.example.english_exam.models.UserVocabulary;
import com.example.english_exam.repositories.UserVocabularyRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserVocabularyService {

    private final UserVocabularyRepository repository;
    private final AuthUtils authUtils;

    public List<UserVocabularyResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserVocabularyResponse findById(Long id) {
        UserVocabulary uv = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User vocabulary không tồn tại"));
        return toResponse(uv);
    }

    public UserVocabularyResponse create(UserVocabularyRequest request, HttpServletRequest httpRequest) {
        Long userId = authUtils.getUserId(httpRequest);
        UserVocabulary uv = new UserVocabulary(userId, request.getVocabId());
        if (request.getStatus() != null) uv.setStatus(request.getStatus());
        uv = repository.save(uv);
        return toResponse(uv);
    }

    public UserVocabularyResponse update(Long id, UserVocabularyRequest request) {
        UserVocabulary uv = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User vocabulary không tồn tại"));
        if (request.getVocabId() != null) uv.setVocabId(request.getVocabId());
        if (request.getStatus() != null) uv.setStatus(request.getStatus());
        uv = repository.save(uv);
        return toResponse(uv);
    }

    public void delete(Long id) {
        UserVocabulary uv = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User vocabulary không tồn tại"));
        repository.delete(uv);
    }

    public void deleteAllUserVocabulary() {
        repository.deleteAll();
    }

    private UserVocabularyResponse toResponse(UserVocabulary uv) {
        UserVocabularyResponse res = new UserVocabularyResponse();
        res.setId(uv.getId());
        res.setUserId(uv.getUserId());
        res.setVocabId(uv.getVocabId());
        res.setStatus(uv.getStatus());
        res.setLastReviewed(uv.getLastReviewed());
        res.setCorrectCount(uv.getCorrectCount());
        return res;
    }
}
