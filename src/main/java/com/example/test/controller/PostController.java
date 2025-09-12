package com.example.test.controller;

import com.example.test.models.Posts;

import com.example.test.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class PostController {
    private PostService postService;
    private UserService userService;
    private CommentService commentService;
    private ImageService imageService;
    private CategoryService categoryService;

    @Autowired
    public PostController(PostService postService, UserService userService, CommentService commentService, ImageService imageService, CategoryService categoryService) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;
        this.imageService = imageService;
        this.categoryService = categoryService;
    }

    //hiển thị toàn bộ bài viết đã duyệt
    @GetMapping("/postApprove")
    public ResponseEntity<List<Posts>> getAllPost() {
        List<Posts> postsApproved = postService.getAllPostApproved();
        return ResponseEntity.ok(postsApproved);
    }


    //tìm kiếm toàn bộ theo tiêu đề
    @GetMapping("/search")
    public ResponseEntity<List<Posts>> searchPostsByTitle(@RequestParam String title) {
        List<Posts> posts = postService.searchPostByTile(title);
        return ResponseEntity.ok(posts);
    }

    //Tạo bài viết
    @PostMapping(value = "/posts", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Posts> createBlog(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("userId") Long userId,
            @RequestParam("checkMembership") boolean checkMembership,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {

            // Tạo bài viết mới từ các trường dữ liệu
            Posts post = new Posts();
            post.setTitle(title);
            post.setContent(content);
            post.setUserId(userId);
            post.setCategoryId(categoryId);
            post.setCheckMembership(checkMembership);
            post.setStatus(Posts.PostStatus.Pending);
            // Lưu bài viết vào cơ sở dữ liệu
            Posts createdPost = postService.save(post);
            imageService.uploadImage(file,createdPost.getPost_id());


            // Trả về phản hồi với bài viết đã tạo
            return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    //cập nhật bài viết
    @PutMapping(value ="/posts/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Posts> UpdateBlog(@Valid @PathVariable Long id,
                                            @ModelAttribute Posts post,
                                            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            Posts update = postService.update(id, post,file);
            return ResponseEntity.ok(update);
        } catch (Exception e) {
            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
        }
    }

    //hiển thị toàn bộ bài viết duyệt và chưa duyệt
    @GetMapping("/posts")
    public List<Posts> showPosts() {
        return postService.getAll();
    }

    //hiển thị bài viết theo id
    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id, HttpSession session) {
        // Lấy bài viết từ database
        Optional<Posts> postOptional = postService.getById(id);
        if (!postOptional.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Bài viết không tồn tại");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND); // 404 Not Found
        }

        Posts post = postOptional.get();

        // Kiểm tra nếu checkMembership là true hoặc null
        if (post.getCheckMembership()) {
            // Lấy userId từ session
            Long userId = (Long) session.getAttribute("userId");

            // Kiểm tra nếu userId là null (chưa đăng nhập)
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Bạn cần tài khoản VIP để có thể xem bài viết này.");
                response.put("loginRequired", true);
                response.put("loginLink", "/login");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED); // 401 Unauthorized
            }

            // Kiểm tra quyền truy cập
            if (!userService.canAccessPost(userId, post.getCheckMembership())) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Bạn không có quyền đọc bài viết này. Vui lòng nâng cấp thành viên.");
                response.put("upgradeRequired", true);
                response.put("upgradeLink", "/payment");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN); // 403 Forbidden
            }
        }

        // Trả về nội dung bài viết nếu không yêu cầu đăng nhập hoặc đã vượt qua kiểm tra
        return new ResponseEntity<>(post, HttpStatus.OK); // 200 OK
    }

    //xóa bài viết theo id
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePostById(@PathVariable Long id) {
        Optional<Posts> post = postService.getById(id);
        if (post.isPresent()) {
            postService.deleteById(id);
            commentService.deleteCommentsByPostId(id);
//            imageService.deleteImageByPostId(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    //lấy bài viết theo user id
    @GetMapping("/posts/user/{id}")
    public ResponseEntity<List<Posts>> getPostByUserId(@PathVariable Long id) {
        List<Posts> posts = postService.getPostByUserId(id);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/posts/category/{id}")
    public ResponseEntity<List<Posts>> getPostByCategoryId(@PathVariable Long id) {
        List<Posts> posts = postService.findPostByCategoryId(id);
        return ResponseEntity.ok(posts);

    }
}


