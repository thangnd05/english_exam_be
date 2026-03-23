package com.example.english_exam.repositories;

import com.example.english_exam.models.ClassMember;
import com.example.english_exam.models.ClassMember.MemberStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    // 🔹 Lấy tất cả học sinh trong lớp
    List<ClassMember> findByClassId(Long classId);

    // 🔹 Lấy danh sách học sinh theo trạng thái (pending/approved)
    List<ClassMember> findByClassIdAndStatus(Long classId, MemberStatus status);

    List<ClassMember> findByUserIdAndStatus(Long studentId, MemberStatus status);
    long countByUserIdAndStatus(Long userId, MemberStatus status);

    // 🔹 Kiểm tra học sinh đã trong lớp chưa
    boolean existsByClassIdAndUserId(Long classId, Long userId);

    // 🔹 Duyệt 1 học sinh (UPDATE status = APPROVED)
    @Modifying
    @Query("UPDATE ClassMember c SET c.status = 'APPROVED' WHERE c.classId = :classId AND c.userId = :userId AND c.status = 'PENDING'")
    int approveSingle(@Param("classId") Long classId, @Param("userId") Long userId);

    // 🔹 Duyệt tất cả học sinh đang chờ duyệt trong lớp
    @Modifying
    @Query("UPDATE ClassMember c SET c.status = 'APPROVED' WHERE c.classId = :classId AND c.status = 'PENDING'")
    int approveAllPending(@Param("classId") Long classId);

    // 🔹 Xóa 1 học sinh khỏi lớp
    @Modifying
    @Query("DELETE FROM ClassMember c WHERE c.classId = :classId AND c.userId = :userId")
    int removeStudent(@Param("classId") Long classId, @Param("userId") Long userId);
}
