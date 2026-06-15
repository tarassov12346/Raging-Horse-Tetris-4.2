package com.app.game.tetris.gameArtefactserviceImpl;

import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.model.State;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class GameArtefactServiceImpl implements GameArtefactService {

    @Value("${shotsPath}")
    private String shotsPath;

    private final Browser browser;
    private final String baseUrl;

    // 🔥 РЕШЕНИЕ ДЛЯ LOOM: Выделенный пул платформенных (OS) потоков для изоляции блокирующего JNI/Playwright ввода-вывода.
    // Это гарантирует, что тяжелый Chromium-рендеринг никогда не зажмет виртуальные потоки Loom.
    private final ExecutorService playwrightExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "playwright-render-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
    );

    public GameArtefactServiceImpl(Browser browser, @Value("${app.base-url}") String baseUrl) {
        this.browser = browser;
        this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<Void> makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore) {
        // 🔥 Обертываем выполнение в CompletableFuture, чтобы вызывающий поток мог дождаться окончания записи файла
        return CompletableFuture.runAsync(
                () -> executeSnapshotGeneration(fileNameDetail, playGameService, state, bestPlayerName, bestPlayerScore),
                playwrightExecutor
        );
    }


    private void executeSnapshotGeneration(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        String format = "jpg";
        String fileName = pathToShots + fileNameDetail + "." + format;

        // 🔥 УБРАЛИ ГЛОБАЛЬНЫЙ LOCK: Каждый поток работает в своем BrowserContext параллельно!
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            page.context().addInitScript("localStorage.setItem('jwt_token', 'fake-token')");

            char[][] cellsCharMatrix = playGameService.drawTetraminoOnCells(state);
            java.util.List<String> cellsList = java.util.stream.Stream.of(cellsCharMatrix)
                    .map(String::new)
                    .toList();

            page.navigate(baseUrl + "/html/snapShot.html");
            page.waitForSelector("#c19v11");

            String massiveJsInject = """
                (data) => {
                    let cells = data.cells;
                    let baseUrl = data.baseUrl;

                    for (let i = 0; i < 20; i++) {
                        for (let j = 0; j < 12; j++) {
                            let cellId = 'c' + i + 'v' + j;
                            let cellImg = document.getElementById(cellId);
                            if (cellImg && cells[i]) {
                                cellImg.src = baseUrl + '/img/' + cells[i][j] + '.png';
                            }
                        }
                    }
                    document.getElementById('gameStatusBox').innerHTML = data.status;
                    document.getElementById('playerBox').innerHTML = data.player;
                    document.getElementById('playerScoreBox').innerHTML = data.score;
                    document.getElementById('bestPlayerBox').innerHTML = data.bestPlayer;
                    document.getElementById('bestPlayerScoreBox').innerHTML = data.bestScore;
                    document.getElementById('tetrisSpeedBox').innerHTML = data.speed;
                }
            """;

            java.util.Map<String, Object> data = java.util.Map.of(
                    "cells", cellsList,
                    "baseUrl", baseUrl,
                    "status", "Game OVER!!!",
                    "player", state.getGame().getPlayerName(),
                    "score", String.valueOf(state.getGame().getPlayerScore()),
                    "bestPlayer", bestPlayerName,
                    "bestScore", String.valueOf(bestPlayerScore),
                    "speed", "Tetris at speed " + (state.getGame().getPlayerScore() / 10)
            );

            page.evaluate(massiveJsInject, data);

            page.waitForSelector("#gameStatusBox", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("#c19v11");

            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(fileName)));
            log.info("📸 [Playwright] Скриншот успешно сгенерирован: {}", fileNameDetail);

        } catch (com.microsoft.playwright.PlaywrightException e) {
            if (e.getMessage().contains("__adopt__")) {
                // 🔥 ИСПРАВЛЕНИЕ УТЕЧКИ: Немедленный return! Предотвращаем каскадные ошибки сломанного контекста
                log.warn("⚠️ [Playwright] Внутренний сбой контекста Chromium (adopt), генерация прервана: {}", e.getMessage());
            } else {
                log.error("💥 Критическая ошибка рендеринга Playwright: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("💥 Сбой генерации скриншота для файла {}: ", fileNameDetail, e);
        }
    }

    // Безопасное закрытие пула при остановке приложения Spring Boot
    @PreDestroy
    public void shutdown() {
        log.info("🛑 Завершение работы пула рендеринга Playwright...");
        playwrightExecutor.shutdown();
    }
}
