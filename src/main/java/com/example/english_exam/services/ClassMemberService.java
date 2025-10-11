package com.example.english_exam.services;

import com.example.english_exam.dto.response.ClassStudentResponse;
import com.example.english_exam.models.ClassEntity;
import com.example.english_exam.models.ClassMember;
import com.example.english_exam.models.ClassMember.MemberStatus;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.ClassMemberRepository;
import com.example.english_exam.repositories.ClassRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.security.AuthService;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassMemberService {

    private final ClassMemberRepository classMemberRepository;
    private final AuthUtils authUtils;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    // 🟢 Học sinh gửi yêu cầu tham gia lớp (status = PENDING)
    @Transactional
    public ClassMember joinClass(Long classId, HttpServletRequest request) {
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

        return classMemberRepository.save(member);
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


    // 🟢 Lấy tất cả học sinh trong lớp
    public List<ClassMember> getAllMembers(Long classId) {
        return classMemberRepository.findByClassId(classId);
    }

    // 🟢 Lấy danh sách học sinh đang chờ duyệt
    public List<ClassMember> getPendingMembers(Long classId) {
        return classMemberRepository.findByClassIdAndStatus(classId, MemberStatus.PENDING);
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

    public List<ClassStudentResponse> getClassesOfCurrentStudent(HttpServletRequest request) {
        Long currentUserId = authUtils.getUserId(request);

        // 🔹 Lấy danh sách class_member mà học sinh đã được duyệt
        List<ClassMember> classMembers =
                classMemberRepository.findByUserIdAndStatus(currentUserId, ClassMember.MemberStatus.APPROVED);

        // 🔹 Chuyển từng bản ghi thành DTO có className + teacherName
        return classMembers.stream().map(member -> {
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
    }



}
