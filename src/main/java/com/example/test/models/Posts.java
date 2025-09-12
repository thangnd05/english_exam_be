package com.example.test.models;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "posts")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "post_id")  // Thêm ID generator
public class Posts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long post_id;

    private String title;

    @Lob // @Lob cho phép lưu trữ văn bản dài
    @Column(name = "content",length = 500000000)
    private String content;

    //Nếu bạn muốn cho phép một số định dạng ngày tháng tùy chỉnh, bạn có thể sử dụng annotation
    // @DateTimeFormat trong Spring Boot để chỉ định định dạng cho LocalDate.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate created_at;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate updated_at;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "check_membership")
    private Boolean checkMembership;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PostStatus status; // Trạng thái của bài viết (pending, approved, rejected)


    public enum PostStatus {
        Pending,Approved
    }

    @PrePersist
    public void setDefaultStatus() {
        if (this.status == null) {
            this.status = PostStatus.Pending;
        }
    }







}