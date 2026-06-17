package com.app.game.tetris.configuration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.net.http.HttpClient;

@Configuration
public class RestTemplateConfiguration {

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 🔥 РЕШЕНИЕ ДЛЯ LOOM: Явно подключаем клиент на базе java.net.http.HttpClient.
        // Он полностью написан на Java 21, избавлен от нативных блокировок ОС
        // и гарантирует идеальный неблокирующий Offloading внутри виртуальных потоков.
        return builder
                .requestFactory(() -> new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .build()
                ))
                .build();
    }
}

