package com.example.english_exam.services.ApiExtend;

import com.example.english_exam.models.Answer;
import com.example.english_exam.models.Passage; // Thêm import cho Passage
import com.example.english_exam.models.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * THAY ĐỔI 1: Sửa chữ ký hàm để nhận thêm đối tượng Passage.
     * Tham số 'passage' có thể là null.
     */
    public String explainQuestion(Question question, List<Answer> answers, Passage passage) {
        logger.info("Đang gọi Gemini API để giải thích câu hỏi ID: {}", question.getQuestionId());

        // --- BẮT ĐẦU XÂY DỰNG PROMPT ---
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là một trợ lý chuyên giải thích các câu hỏi trắc nghiệm tiếng Anh. Hãy trả lời bằng định dạng HTML.\n");
        prompt.append("Yêu cầu:\n");
        prompt.append("1. Phân tích câu hỏi và từng đáp án dựa trên ngữ cảnh được cung cấp (nếu có).\n");
        prompt.append("2. Sử dụng thẻ <strong> để in đậm các tiêu đề (ví dụ: 'Phân tích câu hỏi:', 'Đáp án đúng:', 'A:', 'B:', ...).\n");
        prompt.append("3. CHỈ in đậm TOÀN BỘ phần giải thích của đáp án ĐÚNG. Các đáp án sai KHÔNG được in đậm bất kỳ từ nào trong phần giải thích.\n");
        prompt.append("4. Trả lời theo đúng cấu trúc HTML template sau đây:\n\n");

        // HTML Template
        prompt.append("<div>\n");
        prompt.append("  <p><strong>Phân tích câu hỏi:</strong> [Giải thích ngắn gọn về mục tiêu của câu hỏi dựa trên ngữ cảnh]</p>\n");
        prompt.append("  <p><strong>Đáp án đúng:</strong> [Chỉ ghi ký tự của đáp án đúng]</p>\n");
        prompt.append("  <strong>Giải thích chi tiết:</strong>\n");
        prompt.append("  <ul>\n");
        prompt.append("    <li><strong>A:</strong> [Giải thích tại sao A đúng hoặc sai. Nếu đúng, toàn bộ giải thích này phải được bọc trong thẻ <strong>]</li>\n");
        prompt.append("    <li><strong>B:</strong> [Giải thích tại sao B đúng hoặc sai]</li>\n");
        prompt.append("    <li><strong>C:</strong> [Giải thích tại sao C đúng hoặc sai]</li>\n");
        prompt.append("    <li><strong>D:</strong> [Giải thích tại sao D đúng hoặc sai]</li>\n");
        prompt.append("  </ul>\n");
        prompt.append("</div>\n");

        prompt.append("----------------\n");

        /**
         * THAY ĐỔI 2: Thêm ngữ cảnh từ Passage vào prompt nếu passage tồn tại.
         * Đây là phần quan trọng nhất để AI hiểu được câu hỏi đọc/nghe hiểu.
         */
        if (passage != null && passage.getContent() != null && !passage.getContent().isEmpty()) {
            prompt.append("DỮ LIỆU NGỮ CẢNH (ĐOẠN VĂN):\n");
            prompt.append(passage.getContent()).append("\n\n");
        } else if (passage != null && passage.getMediaUrl() != null && !passage.getMediaUrl().isEmpty()) {
            prompt.append("DỮ LIỆU NGỮ CẢNH: Đây là một câu hỏi dựa trên một bài nghe.\n\n");
        }

        // Thêm dữ liệu câu hỏi và các câu trả lời
        prompt.append("DỮ LIỆU CÂU HỎI:\n");
        prompt.append("Câu hỏi: ").append(question.getQuestionText()).append("\n");

        for (Answer a : answers) {
            prompt.append("- ")
                    .append(a.getAnswerLabel())
                    .append(": ")
                    .append(a.getAnswerText());
            if (a.getIsCorrect()) {
                prompt.append(" (Đây là đáp án đúng)");
            }
            prompt.append("\n");
        }

        logger.debug("Prompt được tạo: {}", prompt.toString());

        // --- PHẦN GỌI API GIỮ NGUYÊN ---
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt.toString());

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.5);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    geminiApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Gọi API thành công, nhận được phản hồi.");
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> contentWrapper = (Map<String, Object>) firstCandidate.get("content");
                    if (contentWrapper != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentWrapper.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
                logger.warn("Phản hồi từ Gemini API có cấu trúc không mong đợi: {}", response.getBody());
                return "Lỗi: Không phân tích được phản hồi từ Gemini.";
            } else {
                logger.error("Gọi API không thành công. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return "API call thất bại với mã lỗi: " + response.getStatusCode();
            }
        } catch (HttpClientErrorException e) {
            logger.error("Lỗi HTTP khi gọi Gemini API. Status: {}. Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "API call thất bại: " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            logger.error("Lỗi kết nối khi gọi Gemini API: {}", e.getMessage(), e);
            return "API call thất bại do lỗi kết nối mạng.";
        }
    }
}