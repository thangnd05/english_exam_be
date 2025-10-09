package com.example.english_exam.controllers;

import com.example.english_exam.models.ClassMember;
import com.example.english_exam.services.ClassMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class-members")
@RequiredArgsConstructor
public class ClassMemberController {

    private final ClassMemberService classMemberService;

    // 🟢 Học sinh gửi yêu cầu tham gia lớp (JWT tự lấy userId)
    @PostMapping("/join")
    public ResponseEntity<?> joinClass(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Long classId = body.get("classId");
            ClassMember member = classMemberService.joinClass(classId, request);
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Học sinh rời khỏi lớp
    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveClass(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Long classId = body.get("classId");
            classMemberService.leaveClass(classId, request);
            return ResponseEntity.ok(Map.of("message", "You have left the class successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Giáo viên duyệt 1 học sinh
    @PutMapping("/approve")
    public ResponseEntity<?> approveSingle(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Long classId = body.get("classId");
            Long userId = body.get("userId");
            classMemberService.approveSingle(classId, userId, request);
            return ResponseEntity.ok(Map.of("message", "Member approved successfully"));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }


    // 🟢 Giáo viên duyệt tất cả học sinh đang chờ trong lớp
    @PutMapping("/approve-all/{classId}")
    public ResponseEntity<?> approveAll(@PathVariable Long classId, HttpServletRequest request) {
        try {
            int count = classMemberService.approveAll(classId, request);
            return ResponseEntity.ok(Map.of("message", "Approved " + count + " pending members"));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }


    // 🟢 Lấy danh sách tất cả học sinh trong lớp
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<ClassMember>> getAllMembers(@PathVariable Long classId) {
        return ResponseEntity.ok(classMemberService.getAllMembers(classId));
    }

    // 🟢 Lấy danh sách học sinh đang chờ duyệt
    @GetMapping("/class/{classId}/pending")
    public ResponseEntity<List<ClassMember>> getPendingMembers(@PathVariable Long classId) {
        return ResponseEntity.ok(classMemberService.getPendingMembers(classId));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeMember(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Long classId = body.get("classId");
            Long userId = body.get("userId");
            classMemberService.removeMember(classId, userId, request);
            return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
        } catch (RuntimeException e) {
            int status = e.getMessage().contains("authorized") ? 403 : 400;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

}
