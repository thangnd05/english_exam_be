package com.example.english_exam.controllers;

import com.example.english_exam.models.ExamType;
import com.example.english_exam.services.ExamTypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exam-types")
public class ExamTypeController {

    private final ExamTypeService examTypeService;

    public ExamTypeController(ExamTypeService examTypeService) {
        this.examTypeService = examTypeService;
    }

    @GetMapping
    public List<ExamType> getAll() {
        return examTypeService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ExamType> getById(@PathVariable Long id) {
        return examTypeService.findById(id);
    }

    @PostMapping
    public ExamType create(@RequestBody ExamType examType) {
        return examTypeService.save(examType);
    }

    @PutMapping("/{id}")
    public ExamType update(@PathVariable Long id, @RequestBody ExamType examType) {
        examType.setExamTypeId(id);
        return examTypeService.save(examType);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        examTypeService.deleteById(id);
    }
}
