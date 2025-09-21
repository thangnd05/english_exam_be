package com.example.english_exam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Username hoặc Email không được để trống")
    private String identifier;

    @NotBlank(message = "Password không được để trống")
    private String password;
}

