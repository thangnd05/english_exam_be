package com.example.test.controller;

import com.example.test.models.Comments;
import com.example.test.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CommentController {
    private CommentService commentService;
    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/comment")
    public List<Comments> showComment() {
        return commentService.getAll();
    }

    @GetMapping("/comment/{id}")
    public ResponseEntity<Comments>CommentById( @PathVariable(required = false) Long id) {
        Optional<Comments>comments=commentService.getByIdComment(id);
//        if(comments.isPresent()){
            return new ResponseEntity<>(comments.get(),HttpStatus.OK);
//        }else{
//            return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
    }
    @PostMapping("/comment")
    public ResponseEntity<Comments>createComment(@Valid @RequestBody Comments comment){
        try {
            Comments create = commentService.Create(comment);
            return new ResponseEntity<>(create, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
        }
    }
    @PutMapping("/comment/{id}")
    public ResponseEntity<Comments>UpdateUsers(@Valid @PathVariable Long id, @RequestBody Comments comments) {
        try {
            Comments update = commentService.update(id,comments);
            return ResponseEntity.ok(update);
        } catch (Exception e) {
            return ResponseEntity.status((HttpStatus.INTERNAL_SERVER_ERROR)).build();
        }
    }
    @DeleteMapping("/comment/{id}")
    public ResponseEntity<Void>deleteCommentId(@PathVariable Long id){
        Optional<Comments> comment = commentService.getByIdComment(id);
        if (comment.isPresent()) {
            commentService.deleteCommentId(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("posts/{postId}/comments")
    public ResponseEntity<List<Comments>> getCommentsByPostId(@PathVariable("postId") Long postId) {
        List<Comments> comments = commentService.getCommentByPost_id(postId);
//        if (comments.isEmpty()) {
//            return ResponseEntity.noContent().build();
//        }
        return new ResponseEntity<>(comments,HttpStatus.OK);
    }

    @GetMapping("comment/user/{id}")
    public  ResponseEntity<List<Comments>>getCommentByUserId(@PathVariable Long id){
        List<Comments>comments=commentService.getCommentByUserId(id);
//        if (comments == null || comments.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
        return new ResponseEntity<>(comments,HttpStatus.OK);
    }

    @PostMapping("/comment/{parentId}/reply")
    public ResponseEntity<Comments> replyComment(
            @PathVariable Long parentId,
            @Valid @RequestBody Comments reply
    ) {
        try {
            reply.setParentId(parentId);
            Comments createdReply = commentService.Create(reply);
            return new ResponseEntity<>(createdReply, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }







}
