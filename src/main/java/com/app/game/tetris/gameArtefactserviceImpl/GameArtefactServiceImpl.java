package com.app.game.tetris.gameArtefactserviceImpl;

import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.model.State;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class GameArtefactServiceImpl implements GameArtefactService {

    @Value("${shotsPath}")
    private String shotsPath;

    private final Browser browser;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Заменяем synchronized на ReentrantLock, дружелюбный к виртуальным потокам
    private final ReentrantLock lock = new ReentrantLock();

    public GameArtefactServiceImpl(Browser browser, @Value("${app.base-url}") String baseUrl) {
        this.browser = browser;
        this.baseUrl = baseUrl;
    }

    @Override
    public void makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        String format = "jpg";
        String fileName = pathToShots + fileNameDetail + "." + format;

        lock.lock();
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            page.context().addInitScript("localStorage.setItem('jwt_token', 'fake-token')");

            // 1. Получаем нативную char[][] матрицу стакана
            char[][] cellsCharMatrix = playGameService.drawTetraminoOnCells(state);

            // 2. Конвертируем char[][] в List<String>, который Playwright гарантированно умеет сериализовать
            java.util.List<String> cellsList = java.util.stream.Stream.of(cellsCharMatrix)
                    .map(String::new)
                    .toList();

            page.navigate(baseUrl + "/html/snapShot.html");
            page.waitForSelector("#c19v11");

            // 3. Скрипт JavaScript (теперь cells — это массив строк, к символам строки в JS обращаемся как cells[i][j])
            String massiveJsInject = """
            (data) => {
                let cells = data.cells;
                let baseUrl = data.baseUrl;

                for (let i = 0; i < 20; i++) {
                    for (let j = 0; j < 12; j++) {
                        let cellId = 'c' + i + 'v' + j;
                        let cellImg = document.getElementById(cellId);
                        if (cellImg && cells[i]) {
                            // Прокатит идеально: cells[i] — это строка, cells[i][j] — j-й символ
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

            // Передаем cellsList вместо сырого char[][] массивов
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

        } catch (com.microsoft.playwright.PlaywrightException e) {
            if (e.getMessage().contains("__adopt__")) {
                log.warn("Playwright internal error (ignored): {}", e.getMessage());
            } else {
                log.error("Критическая ошибка Playwright: {}", e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            log.error("Сбой генерации скриншота: ", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }


}
