package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.ChapterRequest;
import com.example.english_exam.dto.response.ChapterResponse;
import com.example.english_exam.services.ChapterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@AllArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    public ResponseEntity<ChapterResponse> create(
            HttpServletRequest requestHttp,
            @RequestBody ChapterRequest request
    ) {
        return ResponseEntity.ok(chapterService.create(requestHttp, request));
    }

    @GetMapping
    public ResponseEntity<List<ChapterResponse>> getAll() {
        return ResponseEntity.ok(chapterService.getAll());
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ChapterResponse>> getByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(chapterService.getByClassId(classId));
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterResponse> getById(@PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getById(chapterId));
    }

    @PatchMapping("/{chapterId}")
    public ResponseEntity<ChapterResponse> update(
            HttpServletRequest requestHttp,
            @PathVariable Long chapterId,
            @RequestBody ChapterRequest request
    ) {
        return ResponseEntity.ok(chapterService.update(requestHttp, chapterId, request));
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<?> delete(
            HttpServletRequest requestHttp,
            @PathVariable Long chapterId
    ) {
        chapterService.delete(requestHttp, chapterId);
        return ResponseEntity.ok("Deleted successfully");
    }

}
