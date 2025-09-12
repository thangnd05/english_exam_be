package com.example.test.controller;


import com.example.test.models.Posts;
import com.example.test.services.CommentService;
import com.example.test.services.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private PostService postService;

    public AdminController(PostService postService) {
        this.postService = postService;

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public ResponseEntity<Posts> postApproved(@Valid @PathVariable Long id) {
        try {
            Posts approvedPost = postService.Approved(id);
            return ResponseEntity.ok(approvedPost);
        } catch (Exception e) {
            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
        }
    }
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/posts")
//    public List<Posts> showPosts() {
//        return postService.getAll();
//    }
@GetMapping("/posts")
public List<Posts> showPosts() {
    return postService.getAll();
}
}
