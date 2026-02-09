package com.example.english_exam.controllers;

import com.example.english_exam.dto.response.ClassSimpleResponse;
import com.example.english_exam.dto.response.user.TestResponse;
import com.example.english_exam.models.ClassEntity;
import com.example.english_exam.models.Test;
import com.example.english_exam.services.ClassService;
import com.example.english_exam.services.ExamAndTest.TestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;
    private final TestService testService;

    // 🟢 Tạo lớp học mới (teacherId lấy từ token)
    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody ClassEntity classEntity, HttpServletRequest request) {
        try {
            ClassEntity created = classService.createClass(classEntity, request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Lấy tất cả lớp của giáo viên hiện tại (từ JWT)
    @GetMapping("/my")
    public ResponseEntity<?> getMyClasses(HttpServletRequest request) {
        try {
            List<ClassSimpleResponse> responses = classService.getMyClasses(request);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Sửa thông tin lớp học
    @PutMapping("/{classId}")
    public ResponseEntity<?> updateClass(
            @PathVariable Long classId,
            @RequestBody ClassEntity updated,
            HttpServletRequest request) {
        try {
            ClassEntity result = classService.updateClass(classId, updated, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("authorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // 🟢 Lấy tất cả lớp theo teacherId (cho admin hoặc quản trị)
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<ClassEntity>> getClassesByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(classService.getClassesByTeacher(teacherId));
    }

    // 🟢 Lấy thông tin chi tiết của 1 lớp
    @GetMapping("/{classId}")
    public ResponseEntity<?> getById(@PathVariable Long classId) {
        try {
            return ResponseEntity.ok(classService.getById(classId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Xóa lớp học (giáo viên hiện tại chỉ được xóa lớp của mình)
    @DeleteMapping("/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable Long classId, HttpServletRequest request) {
        try {
            Long teacherId = classService.getCurrentTeacherId(request);
            ClassEntity clazz = classService.getById(classId);

            if (!clazz.getTeacherId().equals(teacherId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You do not own this class"));
            }

            classService.deleteClass(classId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/{classId}/chapters/{chapterId}/tests")
    public ResponseEntity<?> getTestsByClassAndChapter(@PathVariable Long classId,@PathVariable Long chapterId, HttpServletRequest request) {
        List<Test> tests = testService.getTestByClassIdAndChapterId(classId,chapterId, request);

        if (tests.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có bài test nào trong lớp này"));
        }

        return ResponseEntity.ok(tests.stream().map(TestResponse::new).toList());
    }
}
