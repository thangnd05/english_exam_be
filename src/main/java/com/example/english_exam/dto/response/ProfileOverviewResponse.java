package com.example.english_exam.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileOverviewResponse {
    private Long userId;
    private String userName;
    private String fullName;
    private String email;
    private String avatarUrl;
    private Boolean verified;
    private Long roleId;
    private String roleName;
    private LocalDateTime createdAt;
    private TestStats testStats;
    private VocabularyStats vocabularyStats;
    private ClassStats classStats;

    @Getter
    @Builder
    public static class TestStats {
        private Long totalAttempts;
        private Long completedAttempts;
        private Long inProgressAttempts;
        private Integer bestScore;
        private Double averageScore;
        private LocalDateTime lastAttemptAt;
    }

    @Getter
    @Builder
    public static class VocabularyStats {
        private Long totalVocabulary;
        private Long learningVocabulary;
        private Long masteredVocabulary;
    }

    @Getter
    @Builder
    public static class ClassStats {
        private Long approvedClassCount;
        private Long pendingClassCount;
    }
}
