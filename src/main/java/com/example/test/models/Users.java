package com.example.test.models;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "user_id")  // Thêm ID generator

public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long user_id;

    private String fullname;
    private String username;
    private String password;
    private String email;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy ")
    private LocalDate created_at;

    @Column(name = "role")
    @Enumerated(EnumType.STRING) // Lưu dưới dạng chuỗi trong DB
    private Role role ; // Thêm thuộc tính role

    private String resetToken; // Token để reset mật khẩu

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")

    private LocalDateTime resetTokenExpiry; // Thời gian hết hạn của token

    @Column(name = "membership_id")
    private Long membershipId;



    public enum Role {
        USER, ADMIN
    }



}