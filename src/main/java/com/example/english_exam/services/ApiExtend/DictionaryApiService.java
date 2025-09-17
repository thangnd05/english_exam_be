package com.example.english_exam.services.ApiExtend;

import com.example.english_exam.models.DictionaryResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class DictionaryApiService {
    private final RestTemplate restTemplate = new RestTemplate();

    public DictionaryResult fetchWordInfo(String word) {
        String text = null;   // lưu IPA
        String audio = null;  // lưu link audio (ưu tiên Google TTS)

        try {
            // 🔗 API dictionaryapi.dev để lấy phonetic
            String url = "https://api.dictionaryapi.dev/api/v2/entries/en/"
                    + URLEncoder.encode(word, StandardCharsets.UTF_8);

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();

                if (body != null && body.isArray() && body.size() > 0) {
                    JsonNode first = body.get(0);

                    // Lấy phonetic từ mảng phonetics
                    JsonNode phonetics = first.path("phonetics");
                    for (JsonNode phonetic : phonetics) {
                        String textCandidate = phonetic.path("text").asText();
                        if (text == null || text.isEmpty()) {
                            text = textCandidate;
                        }
                    }

                    // Nếu không có thì fallback lấy từ root
                    if (text == null || text.isEmpty()) {
                        text = first.path("phonetic").asText();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching word info: " + e.getMessage());
        }

        // 🔊 Luôn tạo link Google TTS cho audio (đảm bảo không null)
        audio = "https://translate.google.com/translate_tts?ie=UTF-8&q="
                + URLEncoder.encode(word, StandardCharsets.UTF_8)
                + "&tl=en&client=tw-ob";

        return new DictionaryResult(text, audio);
    }
}
