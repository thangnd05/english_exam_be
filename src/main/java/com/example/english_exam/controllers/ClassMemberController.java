package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.ClassMemberActionRequest;
import com.example.english_exam.dto.request.ClassMemberJoinRequest;
import com.example.english_exam.dto.response.ClassMemberResponse;
import com.example.english_exam.services.ClassMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class-members")
@AllArgsConstructor
public class ClassMemberController {

    private final ClassMemberService classMemberService;

    @PostMapping("/join")
    public ResponseEntity<?> joinClass(@RequestBody ClassMemberJoinRequest body, HttpServletRequest request) {
        try {
            Long classId = body.getClassId();
            if (classId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            ClassMemberResponse member = classMemberService.joinClass(classId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(member);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveClass(@RequestBody ClassMemberJoinRequest body, HttpServletRequest request) {
        try {
            Long classId = body.getClassId();
            if (classId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            classMemberService.leaveClass(classId, request);
            return ResponseEntity.ok(Map.of("message", "You have left the class successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/approve")
    public ResponseEntity<?> approveSingle(@RequestBody ClassMemberActionRequest body, HttpServletRequest request) {
        try {
            Long classId = body.getClassId();
            Long userId = body.getUserId();
            if (classId == null || userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId and userId are required"));
            }
            classMemberService.approveSingle(classId, userId, request);
            return ResponseEntity.ok(Map.of("message", "Member approved successfully"));
        } catch (RuntimeException e) {
            int status = e.getMessage() != null && e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/approve-all/{classId}")
    public ResponseEntity<?> approveAll(@PathVariable Long classId, HttpServletRequest request) {
        try {
            int count = classMemberService.approveAll(classId, request);
            return ResponseEntity.ok(Map.of("message", "Approved " + count + " pending members"));
        } catch (RuntimeException e) {
            int status = e.getMessage() != null && e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ClassMemberResponse>> getAllMembers(@PathVariable Long classId) {
        return ResponseEntity.ok(classMemberService.getAllMembers(classId));
    }

    @GetMapping("/class/{classId}/pending")
    public ResponseEntity<List<ClassMemberResponse>> getPendingMembers(@PathVariable Long classId) {
        return ResponseEntity.ok(classMemberService.getPendingMembers(classId));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeMember(@RequestBody ClassMemberActionRequest body, HttpServletRequest request) {
        try {
            Long classId = body.getClassId();
            Long userId = body.getUserId();
            if (classId == null || userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId and userId are required"));
            }
            classMemberService.removeMember(classId, userId, request);
            return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
        } catch (RuntimeException e) {
            int status = e.getMessage() != null && e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-classes")
    public ResponseEntity<?> getMyClasses(HttpServletRequest request) {
        try {
            Map<String, Object> myClasses = classMemberService.getClassesOfCurrentStudent(request);
            List<?> teaching = (List<?>) myClasses.get("teachingClasses");
            List<?> learning = (List<?>) myClasses.get("learningClasses");
            if ((teaching == null || teaching.isEmpty()) && (learning == null || learning.isEmpty())) {
                return ResponseEntity.ok(Map.of("message", "Bạn chưa có lớp học nào."));
            }
            return ResponseEntity.ok(myClasses);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
