package com.example.english_exam.services;

import com.example.english_exam.dto.response.ClassMemberResponse;
import com.example.english_exam.dto.response.ClassStudentResponse;
import com.example.english_exam.models.ClassEntity;
import com.example.english_exam.models.ClassMember;
import com.example.english_exam.models.ClassMember.MemberStatus;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.ClassMemberRepository;
import com.example.english_exam.repositories.ClassRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassMemberService {

    private final ClassMemberRepository classMemberRepository;
    private final AuthUtils authUtils;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClassMemberResponse joinClass(Long classId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        if (classMemberRepository.existsByClassIdAndUserId(classId, currentUserId)) {
            throw new RuntimeException("You have already requested or joined this class!");
        }

        ClassMember member = ClassMember.builder()
                .classId(classId)
                .userId(currentUserId)
                .status(MemberStatus.PENDING)
                .joinedAt(LocalDateTime.now())
                .build();

        member = classMemberRepository.save(member);
        return toResponse(member);
    }

    // 🟢 Duyệt 1 học sinh (teacher duyệt)
    @Transactional
    public void approveSingle(Long classId, Long userId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        // 🔹 Kiểm tra lớp tồn tại
        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

        // 🔹 Kiểm tra quyền
        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to approve this class!");
        }

        // 🔹 Tiến hành duyệt
        int updated = classMemberRepository.approveSingle(classId, userId);
        if (updated == 0) {
            throw new RuntimeException("Member not found or already approved!");
        }
    }


    // 🟢 Duyệt tất cả học sinh đang chờ trong lớp
    @Transactional
    public int approveAll(Long classId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        // 🔹 Kiểm tra lớp tồn tại
        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

        // 🔹 Kiểm tra quyền (chỉ giáo viên tạo lớp mới được duyệt)
        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to approve all members in this class!");
        }

        // 🔹 Duyệt tất cả học sinh đang chờ
        return classMemberRepository.approveAllPending(classId);
    }


    public List<ClassMemberResponse> getAllMembers(Long classId) {
        return classMemberRepository.findByClassId(classId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassMemberResponse> getPendingMembers(Long classId) {
        return classMemberRepository.findByClassIdAndStatus(classId, MemberStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    private ClassMemberResponse toResponse(ClassMember m) {
        ClassMemberResponse res = new ClassMemberResponse();
        res.setId(m.getId());
        res.setClassId(m.getClassId());
        res.setUserId(m.getUserId());
        res.setStatus(m.getStatus());
        res.setJoinedAt(m.getJoinedAt());
        return res;
    }

    // 🟢 Rút khỏi lớp (student tự rời lớp)
    @Transactional
    public void leaveClass(Long classId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);
        classMemberRepository.removeStudent(classId, currentUserId);
    }

    // 🟢 Giáo viên xóa học sinh khỏi lớp
    @Transactional
    public void removeMember(Long classId, Long userId, HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to remove members from this class!");
        }

        classMemberRepository.removeStudent(classId, userId);
    }

    public Map<String, Object> getClassesOfCurrentStudent(HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        Map<String, Object> result = new HashMap<>();

        // 🧩 1️⃣ Lớp mà tôi đang học (đã được duyệt)
        List<ClassMember> classMembers =
                classMemberRepository.findByUserIdAndStatus(currentUserId, ClassMember.MemberStatus.APPROVED);

        List<ClassStudentResponse> learningClasses = classMembers.stream().map(member -> {
            ClassEntity clazz = classRepository.findById(member.getClassId())
                    .orElse(null);
            if (clazz == null) return null;

            // Lấy tên giáo viên từ teacherId
            String teacherName = userRepository.findById(clazz.getTeacherId())
                    .map(User::getFullName)
                    .orElse("Unknown");

            return new ClassStudentResponse(
                    clazz.getClassId(),
                    clazz.getClassName(),
                    teacherName
            );
        }).filter(Objects::nonNull).toList();


        // 🧩 2️⃣ Lớp mà tôi dạy (nếu là giáo viên)
        List<ClassEntity> teachingClasses = classRepository.findByTeacherId(currentUserId);
        List<ClassStudentResponse> teachingResponses = teachingClasses.stream()
                .map(clazz -> new ClassStudentResponse(
                        clazz.getClassId(),
                        clazz.getClassName(),
                        userRepository.findById(clazz.getTeacherId())
                                .map(User::getFullName)
                                .orElse("Unknown")
                ))
                .toList();


        // ✅ 3️⃣ Trả kết quả gộp
        result.put("teachingClasses", teachingResponses);
        result.put("learningClasses", learningClasses);

        return result;
    }



}
