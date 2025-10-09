package com.example.english_exam.repositories;

import com.example.english_exam.models.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    boolean existsByClassId(Long classId);

    // Nếu muốn lấy danh sách lớp của 1 giáo viên
    List<ClassEntity> findByTeacherId(Long teacherId);
}
