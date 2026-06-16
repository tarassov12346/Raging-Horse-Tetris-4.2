package com.app.game.tetris.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.YamlConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.InputStream;

@Configuration
public class HazelcastConfig {

    @Bean
    public Config tetrisHazelcastConfig() {
        // 🔥 РЕШЕНИЕ: Достаем файл hazelcast.yaml ПРЯМО из папки ресурсов (Classpath)
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("hazelcast.yaml")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Файл конфигурации hazelcast.yaml не найден в паблике resources!");
            }
            Config config = new YamlConfigBuilder(inputStream).build();
            config.setInstanceName("hazelcast-instance");
            return config;
        } catch (Exception e) {
            // Фолбэк на случай сбоя ввода-вывода
            Config defaultConfig = new Config();
            defaultConfig.setInstanceName("hazelcast-instance");
            defaultConfig.setClusterName("raging-horse-tetris-cluster-fallback");
            return defaultConfig;
        }
    }
}


