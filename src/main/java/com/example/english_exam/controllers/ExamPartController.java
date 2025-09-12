package com.example.english_exam.controllers;

import com.example.english_exam.models.ExamPart;
import com.example.english_exam.services.ExamPartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exam-parts")
public class ExamPartController {

    private final ExamPartService examPartService;

    public ExamPartController(ExamPartService examPartService) {
        this.examPartService = examPartService;
    }

    @GetMapping
    public List<ExamPart> getAll() {
        return examPartService.findAll();
    }

    @GetMapping("/by-type/{examTypeId}")
    public List<ExamPart> getByExamType(@PathVariable Long examTypeId) {
        return examPartService.findByExamTypeId(examTypeId);
    }

    @GetMapping("/{id}")
    public Optional<ExamPart> getById(@PathVariable Long id) {
        return examPartService.findById(id);
    }

    @PostMapping
    public ExamPart create(@RequestBody ExamPart examPart) {
        return examPartService.save(examPart);
    }

    @PutMapping("/{id}")
    public ExamPart update(@PathVariable Long id, @RequestBody ExamPart examPart) {
        examPart.setExamPartId(id);
        return examPartService.save(examPart);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        examPartService.deleteById(id);
    }
}
