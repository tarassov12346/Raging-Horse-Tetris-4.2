package com.app.game.tetris.displayservice;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.tetriservice.PlayGameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.ScheduledExecutorService;

public interface DisplayService {
    void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId);

    void sendFinalStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId);

    void sendSavedStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId);

    void sendDaoGameToBeDisplayed(Game game, SimpMessagingTemplate template);

    void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template);
}
