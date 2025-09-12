package com.example.test.respositories;

import com.example.test.models.Posts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostRespo extends JpaRepository<Posts, Long> {
    // Tìm kiếm bài viết có tiêu đề chứa từ khóa 'title', không phân biệt chữ hoa chữ thường
    List<Posts> findByTitleContainingIgnoreCaseAndStatus(String title,Posts.PostStatus status);//viet dung de tim kiem

    //dung de duyet bai viet
    List<Posts> findByStatusOrderByCheckMembershipDesc(Posts.PostStatus status);


    List<Posts> findByCategoryIdAndStatusOrderByCheckMembershipDesc(Long categoryId, Posts.PostStatus status);

    List<Posts>findByUserId(Long UserId);

    List<Posts>findByCategoryId(Long CategoryId);

    @Query("SELECT p FROM Posts p ORDER BY " +
            "CASE p.status WHEN 'Pending' THEN 0 WHEN 'Approved' THEN 1 ELSE 2 END")
    List<Posts> findAllOrderByCustomStatus();





}

