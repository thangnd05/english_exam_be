package com.example.english_exam.services.ApiExtend;

import org.springframework.stereotype.Service;

@Service
public class TextToSpeechService {

    // Hàm generate link audio từ Google Translate TTS
    public String generateAudio(String word) {
        try {
            // Google TTS free API (chỉ cần truyền text và language)
            return "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&q="
                    + word + "&tl=en";
        } catch (Exception e) {
            System.out.println("Error generating TTS: " + e.getMessage());
            return null;
        }
    }
}
