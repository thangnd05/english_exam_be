package com.example.test.respositories;

import com.example.test.models.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comments,Long> {

    List<Comments>findByPostId( Long postId); //can khop vs ten

    List<Comments>findByUserId(Long userId);


    void deleteByPostId(Long postId);
}
