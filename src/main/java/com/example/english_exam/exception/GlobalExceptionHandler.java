package com.example.english_exam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 🔹 Lỗi gửi mail (SMTP)
    @ExceptionHandler(MailException.class)
    public Map<String, Object> handleMailException(MailException ex) {
        // In lỗi chi tiết ra console
        ex.printStackTrace();

        return Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "message", "Không thể kết nối đến máy chủ email. Vui lòng thử lại sau.",
                "error", ex.getClass().getSimpleName(),
                "details", ex.getMessage() // 🧩 thêm dòng này để FE xem chi tiết
        );
    }

    // 🔹 Lỗi logic chung (vd: username hoặc email đã tồn tại, chưa verify, sai mật khẩu...)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException ex) {
        // Ghi log rõ ràng hơn
        System.err.println("❌ RuntimeException: " + ex.getMessage());
        ex.printStackTrace();

        return Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage(),
                "error", ex.getClass().getSimpleName()
        );
    }

    // 🔹 Bắt tất cả lỗi còn lại
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleAllExceptions(Exception ex) {
        System.err.println("🔥 Unhandled Exception: " + ex.getMessage());
        ex.printStackTrace();

        return Map.of(
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message", "Lỗi hệ thống! Vui lòng thử lại sau.",
                "error", ex.getClass().getSimpleName()
        );
    }
}
