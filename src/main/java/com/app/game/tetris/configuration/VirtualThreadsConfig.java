package com.app.game.tetris.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class VirtualThreadsConfig {
    @Primary // 🌟 Заставляет Spring использовать этот экзекутор по умолчанию для всех @Async
    @Bean(name = {"taskExecutor", "applicationTaskExecutor"}) // 🌟 Даем оба имени, чтобы удовлетворить все подсистемы
    public Executor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("loom-");
        // Явно включаем виртуальные потоки Java 21 внутри Spring-экзекутора
        executor.setVirtualThreads(true);
        return executor;
    }
}
