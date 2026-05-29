package com.app.game.tetris.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadsConfig {
    @Bean(name = "loomExecutor")
    public Executor loomExecutor() {
        // Создаем один глобальный экземпляр движка виртуальных потоков
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
