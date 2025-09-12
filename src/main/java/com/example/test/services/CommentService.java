package com.example.test.services;

import com.example.test.models.Comments;
import com.example.test.respositories.CommentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    private CommentRepo commentRepo;

    @Autowired
    public CommentService(CommentRepo commentRepo) {
        this.commentRepo = commentRepo;
    }



    public List<Comments>getAll(){
        return commentRepo.findAll();
    }


    public Optional<Comments> getByIdComment(Long id){
        return commentRepo.findById(id);
    }


    @Transactional
    public Comments Create(Comments comment){
        if(comment.getComment_id()==null){
            comment.setCreated_at(LocalDate.now());
        }
        return commentRepo.save(comment);
    }
    @Transactional
    public void deleteCommentId(Long id){
        commentRepo.deleteById(id);
    }

    public List<Comments>getCommentByPost_id(Long postId){
        return commentRepo.findByPostId(postId);
    }

    @Transactional
    public Comments update(Long id, Comments comments){
        return commentRepo.findById(id).map(commentUpdate -> {
            commentUpdate.setContent(comments.getContent());
            return commentRepo.save(commentUpdate); // Lưu và trả về bản ghi đã cập nhật
        }).orElseThrow(() ->new RuntimeException("Not Found with id:" + id));
    }

    public List<Comments>getCommentByUserId(Long userId){
        return commentRepo.findByUserId(userId);
    }


    @Transactional
    public void deleteCommentsByPostId(Long postId) {
        commentRepo.deleteByPostId(postId);
    }
}
