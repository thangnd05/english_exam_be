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
    long countByTestId(Long testId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, UserTest.Status status);

    int countByUserIdAndTestIdAndStatus(Long userId, Long testId, UserTest.Status status);

    @Query("SELECT ut FROM UserTest ut WHERE ut.userId = :userId AND ut.testId = :testId AND ut.status = :status")
    Optional<UserTest> findActiveUserTest(@Param("userId") Long userId,
                                          @Param("testId") Long testId,
                                          @Param("status") UserTest.Status status);

    List<UserTest> findByUserIdAndTestIdOrderByStartedAtDesc(Long userId, Long testId);


    Optional<UserTest> findTopByUserIdAndTestIdOrderByStartedAtDesc(Long userId, Long testId);
    Optional<UserTest> findTopByUserIdOrderByStartedAtDesc(Long userId);
    Optional<UserTest> findTopByUserIdAndStatusOrderByTotalScoreDesc(Long userId, UserTest.Status status);

    long countByTestIdAndUserId(Long testId, Long userId);

    @Query("SELECT AVG(ut.totalScore) FROM UserTest ut WHERE ut.userId = :userId AND ut.status = :status")
    Double findAverageScoreByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserTest.Status status);

}
