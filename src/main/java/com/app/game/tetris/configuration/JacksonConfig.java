package com.app.game.tetris.configuration;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        // 🔥 РЕШЕНИЕ ДЛЯ JACKSON: Кастомизируем билдер ДО создания инстанса ObjectMapper.
        // Это сохраняет правильный жизненный цикл Spring Boot 3.4 и гарантирует,
        // что модуль JavaTimeModule и поддержка Java 21 рекордов применятся ко ВСЕМ мапперам.
        return builder -> {
            builder.modules(new JavaTimeModule());
            log.info("⚙️ [Jackson-Config] Модуль JavaTimeModule успешно зарегистрирован в Спринговом ObjectMapper Builder");
        };
    }
}
