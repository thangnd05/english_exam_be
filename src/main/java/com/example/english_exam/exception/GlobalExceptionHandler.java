package com.example.english_exam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🔹 Lỗi gửi mail (smtp.gmail.com lỗi)
    @ExceptionHandler(MailException.class)
    public Map<String, Object> handleMailException(MailException ex) {
        return Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "message", "Không thể kết nối đến máy chủ email. Vui lòng thử lại sau.",
                "error", ex.getClass().getSimpleName()
        );
    }

    // 🔹 Lỗi logic chung (vd: username hoặc email đã tồn tại)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        return Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage()
        );
    }
}
