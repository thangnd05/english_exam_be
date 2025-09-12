package com.example.test.services;
import com.example.test.models.Images;
import com.example.test.models.Posts;
import com.example.test.respositories.CommentRepo;
import com.example.test.respositories.ImageRespo;
import com.example.test.respositories.PostRespo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private PostRespo postRespo;
    private ImageService imageService;
    private ImageRespo imageRepository;
    private CommentRepo commentRepo;

    @Autowired
    public PostService(PostRespo postRespo, ImageService imageService, ImageRespo imageRepository, CommentRepo commentRepo) {
        this.postRespo = postRespo;
        this.imageService = imageService;
        this.imageRepository = imageRepository;
        this.commentRepo = commentRepo;
    }

    public List<Posts>getAll() {
        return postRespo.findAllOrderByCustomStatus();
    }



    //xem toàn bộ bài viết đã duyệt
    public List<Posts>getAllPostApproved(){
        return postRespo.findByStatusOrderByCheckMembershipDesc(Posts.PostStatus.Approved);
    }


    public Optional<Posts>getById(Long id){
        return postRespo.findById(id);
    }


    //lưu bài viết
    public Posts save(Posts post) {
        if (post.getPost_id() == null) {
            post.setCreated_at(LocalDate.now());
        }

        post.setUpdated_at(LocalDate.now());
        return postRespo.save(post);
    }


    @Transactional
    //sửa bài viết
    public Posts update(Long id, Posts post, MultipartFile file) throws IOException {
        Optional<Posts> posts = postRespo.findById(id);

        Posts postUpdate=posts.get();

        postUpdate.setStatus(Posts.PostStatus.Pending);
        postUpdate.setTitle(post.getTitle());
        postUpdate.setContent(post.getContent());
        postUpdate.setCategoryId(post.getCategoryId());
        postUpdate.setCheckMembership(post.getCheckMembership());
        postUpdate.setUpdated_at(LocalDate.now());

        Optional<Images> imagesExist =imageRepository.findByPostId(postUpdate.getPost_id());
        Images existingImage=imagesExist.get();
        if (file != null && !file.isEmpty()) {
                if (existingImage != null) {
                    // Cập nhật ảnh hiện tại
                    imageService.updateImage(existingImage.getImage_id(), file);
                } else {
                    // Tạo ảnh mới
                    imageService.uploadImage(file, postUpdate.getPost_id());
                }
            } else if (existingImage == null) {
                // Nếu không có file và không có ảnh hiện tại, gán ảnh mặc định
                imageService.uploadImage(null, postUpdate.getPost_id());
            }
        return postRespo.save(postUpdate);
    }

    //Duyệt bài viết
    public Posts Approved(Long id){
        return postRespo.findById(id).map(postApproved ->{
            postApproved.setStatus(Posts.PostStatus.Approved);
            return postRespo.save(postApproved);
        }).orElseThrow(() ->new RuntimeException("Not Found with id:" + id));
    }

    @Transactional
    public void deleteById(Long id){
        imageRepository.deleteByPostId(id);
        postRespo.deleteById(id);
        commentRepo.deleteByPostId(id);
    }

    //tìm kiếm bài viết bằng tiêu đề
    @Transactional
    public List<Posts>searchPostByTile(String title){
        return postRespo.findByTitleContainingIgnoreCaseAndStatus(title, Posts.PostStatus.Approved);
    }


    public List<Posts>findPostByCategoryId(Long categoryId){
        return postRespo.findByCategoryIdAndStatusOrderByCheckMembershipDesc(categoryId,Posts.PostStatus.Approved);
    }


    //tìm bài viết tất cả bài viết thông qua user
    public List<Posts>getPostByUserId(Long userId){
        return  postRespo.findByUserId(userId);
    }







}
