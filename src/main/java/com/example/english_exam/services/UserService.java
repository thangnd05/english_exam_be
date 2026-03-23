package com.example.english_exam.services;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.response.ProfileOverviewResponse;
import com.example.english_exam.models.ClassMember;
import com.example.english_exam.models.User;
import com.example.english_exam.models.UserTest;
import com.example.english_exam.models.UserVocabulary;
import com.example.english_exam.repositories.ClassMemberRepository;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.repositories.UserTestRepository;
import com.example.english_exam.repositories.UserVocabularyRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private CloudinaryService cloudinaryService;
    private UserTestRepository userTestRepository;
    private UserVocabularyRepository userVocabularyRepository;
    private ClassMemberRepository classMemberRepository;
    private AuthUtils authUtils;


    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User createUser(User user) {

        // ✅ Auto avatar mặc định theo username (giống Google)
        String defaultAvatar =
                "https://ui-avatars.com/api/?name="
                        + user.getUserName()
                        + "&background=random&color=fff";

        user.setAvatarUrl(defaultAvatar);

        return userRepository.save(user);
    }


    public User updateUser(Long id, User updatedUser, MultipartFile avatar) throws IOException {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Update thông tin profile
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setUserName(updatedUser.getUserName());
        existingUser.setEmail(updatedUser.getEmail());

        // ✅ Upload avatar nếu có
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadImage(avatar);
            existingUser.setAvatarUrl(avatarUrl);
        }

        return userRepository.save(existingUser);
    }

    public ProfileOverviewResponse getProfileOverview(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long totalAttempts = userTestRepository.countByUserId(id);
        long completedAttempts = userTestRepository.countByUserIdAndStatus(id, UserTest.Status.COMPLETED);
        long inProgressAttempts = userTestRepository.countByUserIdAndStatus(id, UserTest.Status.IN_PROGRESS);

        Integer bestScore = userTestRepository.findTopByUserIdAndStatusOrderByTotalScoreDesc(id, UserTest.Status.COMPLETED)
                .map(UserTest::getTotalScore)
                .orElse(0);

        Double averageScore = userTestRepository.findAverageScoreByUserIdAndStatus(id, UserTest.Status.COMPLETED);
        LocalDateTime lastAttemptAt = userTestRepository.findTopByUserIdOrderByStartedAtDesc(id)
                .map(UserTest::getStartedAt)
                .orElse(null);

        long totalVocabulary = userVocabularyRepository.countByUserId(id);
        long learningVocabulary = userVocabularyRepository.countByUserIdAndStatus(id, UserVocabulary.Status.learning);
        long masteredVocabulary = userVocabularyRepository.countByUserIdAndStatus(id, UserVocabulary.Status.mastered);

        long approvedClassCount = classMemberRepository.countByUserIdAndStatus(id, ClassMember.MemberStatus.APPROVED);
        long pendingClassCount = classMemberRepository.countByUserIdAndStatus(id, ClassMember.MemberStatus.PENDING);

        return ProfileOverviewResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .verified(user.getVerified())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .testStats(ProfileOverviewResponse.TestStats.builder()
                        .totalAttempts(totalAttempts)
                        .completedAttempts(completedAttempts)
                        .inProgressAttempts(inProgressAttempts)
                        .bestScore(bestScore)
                        .averageScore(averageScore == null ? 0D : averageScore)
                        .lastAttemptAt(lastAttemptAt)
                        .build())
                .vocabularyStats(ProfileOverviewResponse.VocabularyStats.builder()
                        .totalVocabulary(totalVocabulary)
                        .learningVocabulary(learningVocabulary)
                        .masteredVocabulary(masteredVocabulary)
                        .build())
                .classStats(ProfileOverviewResponse.ClassStats.builder()
                        .approvedClassCount(approvedClassCount)
                        .pendingClassCount(pendingClassCount)
                        .build())
                .build();
    }

    public ProfileOverviewResponse getMyProfileOverview(HttpServletRequest httpRequest) {
        Long userId = authUtils.getUserId(httpRequest);
        return getProfileOverview(userId);
    }



    public boolean deleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }

}
