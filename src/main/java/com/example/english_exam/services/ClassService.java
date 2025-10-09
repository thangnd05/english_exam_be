package com.example.english_exam.services;

import com.example.english_exam.models.ClassEntity;
import com.example.english_exam.repositories.ClassRepository;
import com.example.english_exam.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final AuthService authService;

    // 🟢 Tạo lớp học mới (sinh ID ngẫu nhiên & gán teacherId từ token)
    @Transactional
    public ClassEntity createClass(ClassEntity classEntity, HttpServletRequest httpRequest) {
        // 🔹 Lấy userId hiện tại từ JWT
        Long currentUserId = authService.getCurrentUserId(httpRequest);

        // 🔹 Sinh ID ngẫu nhiên cho class (8 chữ số)
        long randomId;
        do {
            randomId = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        } while (classRepository.existsById(randomId));

        // 🔹 Gán thông tin lớp
        ClassEntity clazz = ClassEntity.builder()
                .classId(randomId)
                .className(classEntity.getClassName())
                .description(classEntity.getDescription())
                .teacherId(currentUserId)
                .createdAt(classEntity.getCreatedAt())
                .build();

        // 🔹 Lưu vào DB
        return classRepository.save(clazz);
    }

    // 🟢 Lấy tất cả lớp của 1 giáo viên
    public List<ClassEntity> getClassesByTeacher(Long teacherId) {
        return classRepository.findByTeacherId(teacherId);
    }

    // 🟢 Lấy thông tin 1 lớp
    public ClassEntity getById(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));
    }

    // 🟢 Xóa lớp (sẽ cascade xóa class_members)
    @Transactional
    public void deleteClass(Long classId) {
        if (!classRepository.existsById(classId)) {
            throw new RuntimeException("Class not found!");
        }
        classRepository.deleteById(classId);
    }

    public Long getCurrentTeacherId(HttpServletRequest request) {
        return authService.getCurrentUserId(request);
    }

    // 🟢 Cập nhật thông tin lớp học
    @Transactional
    public ClassEntity updateClass(Long classId, ClassEntity updated, HttpServletRequest request) {
        Long currentUserId = authService.getCurrentUserId(request);

        // Tìm lớp hiện tại
        ClassEntity existing = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

        // Kiểm tra quyền: chỉ giáo viên tạo lớp mới được sửa
        if (!existing.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to update this class!");
        }

        // Cập nhật thông tin
        existing.setClassName(updated.getClassName());
        existing.setDescription(updated.getDescription());

        // Lưu lại
        return classRepository.save(existing);
    }


}
