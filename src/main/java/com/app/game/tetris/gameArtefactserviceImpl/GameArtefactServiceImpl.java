package com.app.game.tetris.gameArtefactserviceImpl;

import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.model.State;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
public class GameArtefactServiceImpl implements GameArtefactService {

    @Value("${shotsPath}")
    String shotsPath;

    private final Browser browser;
    private final String baseUrl;

    public GameArtefactServiceImpl(Browser browser, @Value("${app.base-url}") String baseUrl) {
        this.browser = browser;
        this.baseUrl = baseUrl;
    }

    @Override
    public void makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        String format = "jpg";
        String fileName = pathToShots + fileNameDetail + "." + format;

        // Оборачиваем всё создание страницы в try-catch
        try (Page page = browser.newPage()) {
            char[][] cells = playGameService.drawTetraminoOnCells(state);
            page.navigate(baseUrl + "/html/snapShot.html");

            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 12; j++) {
                    String cell = "c" + i + "v" + j;
                    String src = baseUrl + "/img/" + cells[i][j] + ".png";
                    String js = "document.getElementById('" + cell + "').src='" + src + "'";
                    page.evaluate(js);
                }
            }

            // Группируем мелкие evaluate в один для стабильности и скорости
            page.evaluate("() => {" +
                    "document.getElementById('gameStatusBox').innerHTML = 'Game OVER!!!';" +
                    "document.getElementById('playerBox').innerHTML = '" + state.getGame().getPlayerName() + "';" +
                    "document.getElementById('playerScoreBox').innerHTML = '" + state.getGame().getPlayerScore() + "';" +
                    "document.getElementById('bestPlayerBox').innerHTML = '" + bestPlayerName + "';" +
                    "document.getElementById('bestPlayerScoreBox').innerHTML = '" + bestPlayerScore + "';" +
                    "document.getElementById('tetrisSpeedBox').innerHTML = 'Tetris at speed " + (state.getGame().getPlayerScore() / 10) + "';" +
                    "}");

            page.waitForSelector("#gameStatusBox", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("#c19v11");

            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(fileName)));

        } catch (com.microsoft.playwright.PlaywrightException e) {
            // Проверяем, является ли это той самой технической ошибкой
            if (e.getMessage().contains("__adopt__")) {
                // Просто логируем и не прерываем работу, так как скриншот скорее всего уже сделан
                System.err.println("Playwright internal error (ignored): " + e.getMessage());
            } else {
                // Если ошибка другая — пробрасываем её дальше
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
