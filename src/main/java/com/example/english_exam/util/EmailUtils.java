//package com.example.english_exam.util;
//
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Map;
//
//@Component
//public class EmailUtils {
//
//    private final RestTemplate restTemplate;
//
//    // URL webhook PRODUCTION (KHÔNG phải webhook-test)
//    private static final String N8N_WEBHOOK_URL =
//            "https://n8ntest.edigital.com.vn/webhook/548f3788-1d6f-4df0-baff-f8c4cc9bed75";
//
//    public EmailUtils() {
//        this.restTemplate = new RestTemplate();
//    }
//
//    public void sendVerificationEmail(String email, String token) {
//        try {
//            Map<String, Object> payload = Map.of(
//                    "email", email,
//                    "verifyUrl", "http://localhost:3000/verify?token=" + token
//            );
//
//            restTemplate.postForEntity(
//                    N8N_WEBHOOK_URL,
//                    payload,
//                    Void.class
//            );
//
//            System.out.println("✅ Gọi n8n gửi email cho: " + email);
//
//        } catch (Exception e) {
//            System.err.println("❌ Lỗi gọi n8n: " + e.getMessage());
//            throw new RuntimeException("Không thể gửi email xác thực");
//        }
//    }
//}
