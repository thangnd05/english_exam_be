package com.example.english_exam.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Bean này tùy chỉnh ObjectMapper mặc định của Spring Boot.
     * Bằng cách này, chúng ta thêm module JavaTimeModule mà không phá vỡ
     * các cấu hình tự động khác của Spring Boot.
     * Đây là cách làm đúng chuẩn và an toàn nhất.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .modules(new JavaTimeModule())
                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // định dạng mặc định cho LocalDateTime
    }

}
