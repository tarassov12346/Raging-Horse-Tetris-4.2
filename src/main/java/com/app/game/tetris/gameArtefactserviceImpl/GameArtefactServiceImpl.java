package com.app.game.tetris.gameArtefactserviceImpl;

import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.model.State;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
public class GameArtefactServiceImpl implements GameArtefactService {

    @Value("${shotsPath}")
    String shotsPath;

    @Override
    public void makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        String format = "jpg";
        String fileName = pathToShots + fileNameDetail + "." + format;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            char[][] cells = playGameService.drawTetraminoOnCells(state);
            page.navigate("http://localhost:8080/html/snapShot.html");
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 12; j++) {
                    String cell = new StringBuilder("c").append(i).append("v").append(j).toString();
                    String src = new StringBuilder("http://localhost:8080/img/").append(cells[i][j]).append(".png").toString();
                    String js = "document.getElementById('" + cell + "').src='" + src + "'";
                    page.evaluate(js);
                }
            }
            playwright.selectors().setTestIdAttribute("id");
            String js = "document.getElementById('gameStatusBox').innerHTML = 'Game OVER!!!'";
            page.evaluate(js);
            js = "document.getElementById('playerBox').innerHTML = '" + state.getGame().getPlayerName() + "'";
            page.evaluate(js);
            js = "document.getElementById('playerScoreBox').innerHTML = '" + state.getGame().getPlayerScore() + "'";
            page.evaluate(js);
            js = "document.getElementById('bestPlayerBox').innerHTML = '" + bestPlayerName + "'";
            page.evaluate(js);
            js = "document.getElementById('bestPlayerScoreBox').innerHTML = '" + bestPlayerScore + "'";
            page.evaluate(js);
            js = "document.getElementById('tetrisSpeedBox').innerHTML = 'Tetris at speed " + state.getGame().getPlayerScore() / 10 + "'";
            page.evaluate(js);
            page.waitForTimeout(3000);
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(fileName)));
        }
    }
}
