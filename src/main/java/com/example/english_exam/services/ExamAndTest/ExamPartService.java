package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.dto.request.ExamPartRequest;
import com.example.english_exam.dto.response.ExamPartResponse;
import com.example.english_exam.models.ExamPart;
import com.example.english_exam.repositories.ExamPartRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ExamPartService {

    private final ExamPartRepository examPartRepository;

    public List<ExamPartResponse> findAll() {
        return examPartRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ExamPartResponse> findByExamTypeId(Long examTypeId) {
        return examPartRepository.findByExamTypeId(examTypeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ExamPartResponse findById(Long id) {
        ExamPart part = examPartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam part không tồn tại"));
        return toResponse(part);
    }

    public ExamPartResponse create(ExamPartRequest request) {
        ExamPart part = new ExamPart();
        part.setExamTypeId(request.getExamTypeId());
        part.setName(request.getName());
        part.setDescription(request.getDescription());
        part.setDefaultNumQuestions(request.getDefaultNumQuestions());
        part.setSkillId(request.getSkillId());
        part = examPartRepository.save(part);
        return toResponse(part);
    }

    public ExamPartResponse update(Long id, ExamPartRequest request) {
        ExamPart part = examPartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam part không tồn tại"));
        if (request.getExamTypeId() != null) part.setExamTypeId(request.getExamTypeId());
        if (request.getName() != null) part.setName(request.getName());
        if (request.getDescription() != null) part.setDescription(request.getDescription());
        if (request.getDefaultNumQuestions() != null) part.setDefaultNumQuestions(request.getDefaultNumQuestions());
        if (request.getSkillId() != null) part.setSkillId(request.getSkillId());
        part = examPartRepository.save(part);
        return toResponse(part);
    }

    public void delete(Long id) {
        ExamPart part = examPartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam part không tồn tại"));
        examPartRepository.delete(part);
    }

    private ExamPartResponse toResponse(ExamPart p) {
        ExamPartResponse res = new ExamPartResponse();
        res.setExamPartId(p.getExamPartId());
        res.setExamTypeId(p.getExamTypeId());
        res.setName(p.getName());
        res.setDescription(p.getDescription());
        res.setDefaultNumQuestions(p.getDefaultNumQuestions());
        res.setSkillId(p.getSkillId());
        return res;
    }
}
