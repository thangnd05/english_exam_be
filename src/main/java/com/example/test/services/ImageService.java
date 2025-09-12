package com.example.test.services;


import com.example.test.cloudinary.CloudinaryService;
import com.example.test.models.Images;
import com.example.test.respositories.ImageRespo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ImageService {

    @Autowired
    private ImageRespo imageRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    private static final String DEFAULT_IMAGE_URL = "https://res.cloudinary.com/drpphz5ue/image/upload/v1748272887/imgdefault.jpg";
    private static final String DEFAULT_IMAGE_PUBLIC_ID = "imgdefault";
    private static final String DEFAULT_IMAGE_NAME = "imgdefault.jpg";

    public Images uploadImage(MultipartFile file, Long postId) throws IOException {
        Images image = new Images();
        image.setPostId(postId);

        if (file == null || file.isEmpty()) {
            // Sử dụng ảnh mặc định nếu không có file
            image.setName(DEFAULT_IMAGE_NAME);
            image.setUrl(DEFAULT_IMAGE_URL);
            image.setPublicId(DEFAULT_IMAGE_PUBLIC_ID);
        } else {
            // Tải ảnh lên Cloudinary nếu có file
            Map uploadResult = cloudinaryService.uploadImage(file);
            String url = (String) uploadResult.get("url");
            String publicId = (String) uploadResult.get("public_id");

            image.setName(file.getOriginalFilename());
            image.setUrl(url);
            image.setPublicId(publicId);
        }

        return imageRepository.save(image);
    }

    public List<Images> getAllImages() {
        return imageRepository.findAll();
    }

    public Optional<Images> getImageById(Long id) {
        return imageRepository.findById(id);
    }

    public Images updateImage(Long id, MultipartFile file) throws IOException {
        Optional<Images> optionalImage = imageRepository.findById(id);
        if (optionalImage.isPresent()) {
            Images image = optionalImage.get();

            if (file == null || file.isEmpty()) {
                // Sử dụng ảnh mặc định nếu không có file
                image.setName(DEFAULT_IMAGE_NAME);
                image.setUrl(DEFAULT_IMAGE_URL);
                image.setPublicId(DEFAULT_IMAGE_PUBLIC_ID);
            } else {
                // Cập nhật ảnh trên Cloudinary
                Map uploadResult = cloudinaryService.updateImage(image.getPublicId(), file);
                String newUrl = (String) uploadResult.get("url");

                image.setName(file.getOriginalFilename());
                image.setUrl(newUrl);
            }
            return imageRepository.save(image);
        }
        throw new RuntimeException("Image not found");
    }

    public void deleteImage(Long id) throws IOException {
        Optional<Images> optionalImage = imageRepository.findById(id);
        if (optionalImage.isPresent()) {
            Images image = optionalImage.get();
            // Chỉ xóa trên Cloudinary nếu không phải ảnh mặc định
            if (!DEFAULT_IMAGE_PUBLIC_ID.equals(image.getPublicId())) {
                cloudinaryService.deleteImage(image.getPublicId());
            }
            imageRepository.deleteById(id);
        } else {
            throw new RuntimeException("Image not found");
        }
    }

    public Optional<Images> getImagesByPostId(Long postId) {
        return imageRepository.findByPostId(postId);
    }
}