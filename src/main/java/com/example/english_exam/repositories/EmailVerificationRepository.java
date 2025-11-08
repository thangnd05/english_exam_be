package com.example.english_exam.repositories;

import com.example.english_exam.models.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByToken(String token);
    List<EmailVerification> findAllByExpiresAtBeforeAndStatus(LocalDateTime time, String status);
    void deleteByUserId(Long userId);
}
