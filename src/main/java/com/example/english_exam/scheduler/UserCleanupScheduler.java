package com.example.english_exam.scheduler;

import com.example.english_exam.services.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



@Component
public class UserCleanupScheduler {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Scheduled(cron = "0 0 3 * * ?") // Mỗi ngày lúc 3h sáng
    public void cleanExpiredUsers() {
        emailVerificationService.cleanExpiredVerifications();
    }
}

