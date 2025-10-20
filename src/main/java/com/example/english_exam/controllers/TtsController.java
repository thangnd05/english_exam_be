package com.example.english_exam.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @CrossOrigin(origins = "*") // ✅ Cho phép FE phát audio (nếu chạy ở port khác)
    @GetMapping
    public void getTts(@RequestParam String text, HttpServletResponse response) throws IOException {
        String encoded = java.net.URLEncoder.encode(text, StandardCharsets.UTF_8);
        String ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&q="
                + encoded + "&tl=en&client=tw-ob";

        // ✅ Tạo kết nối và thêm User-Agent hợp lệ
        HttpURLConnection connection = (HttpURLConnection) new URL(ttsUrl).openConnection();
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.connect();

        // ✅ Gửi header phản hồi cho trình duyệt
        response.setHeader(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setStatus(HttpServletResponse.SC_OK);

        try (var input = connection.getInputStream()) {
            StreamUtils.copy(input, response.getOutputStream());
            response.flushBuffer(); // ✅ đảm bảo dữ liệu gửi đi ngay lập tức
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không phát được âm thanh");
        }
    }
}
