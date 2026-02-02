package com.example.english_exam.services;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.models.User;
import com.example.english_exam.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private CloudinaryService cloudinaryService;


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



    public boolean deleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }

}
