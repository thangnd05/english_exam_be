package com.example.english_exam.repositories;

import com.example.english_exam.models.UserTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTestRepository extends JpaRepository<UserTest, Long> {
    List<UserTest> findByUserId(Long userId);
    List<UserTest> findByTestId(Long testId);

    int countByUserIdAndTestIdAndStatus(Long userId, Long testId, UserTest.Status status);

    @Query("SELECT ut FROM UserTest ut WHERE ut.userId = :userId AND ut.testId = :testId AND ut.status = :status")
    Optional<UserTest> findActiveUserTest(@Param("userId") Long userId,
                                          @Param("testId") Long testId,
                                          @Param("status") UserTest.Status status);

    List<UserTest> findByUserIdAndTestIdOrderByStartedAtDesc(Long userId, Long testId);


    Optional<UserTest> findTopByUserIdAndTestIdOrderByStartedAtDesc(Long userId, Long testId);
}
