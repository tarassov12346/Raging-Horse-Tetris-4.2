package com.app.game.tetris.configuration;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaywrightConfig {
    @Bean(destroyMethod = "close") // Spring вызовет close() при завершении работы
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        // Запускаем браузер один раз в headless-режиме
        return playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
}
