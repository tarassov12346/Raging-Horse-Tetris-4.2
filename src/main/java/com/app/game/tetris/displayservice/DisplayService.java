package com.app.game.tetris.displayservice;

import com.app.game.tetris.dto.GameDataDTO;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.tetriservice.PlayGameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public interface DisplayService {
    void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, SimpMessagingTemplate template, String userId);

    void sendFinalStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String userId);

    void sendSavedStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String userId);

    void sendDaoGameToBeDisplayed(GameDataDTO gameData, SimpMessagingTemplate template, String destinationId);

    void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template, String userId);
}
