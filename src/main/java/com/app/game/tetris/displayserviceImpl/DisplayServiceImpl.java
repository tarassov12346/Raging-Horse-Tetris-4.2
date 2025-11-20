package com.app.game.tetris.displayserviceImpl;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.tetriserviceImpl.State;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;

@Service
public class DisplayServiceImpl implements DisplayService {
    @Override
    public void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId) {
        playGameService.setState(playGameService.createStateAfterMoveDown((State) playGameService.getState(userId), service, gameService), userId);
        char[][] cellsToBeDisplayed = playGameService.getState(userId).getStage().drawTetraminoOnCells();
        State stateToBeSent = playGameService.getState(userId).buildState(playGameService.getState(userId).getStage().buildStage(cellsToBeDisplayed), playGameService.getState(userId).isRunning(), playGameService.getState(userId).getGame());
        template.convertAndSend("/receive/stateObjects", stateToBeSent);
    }

    @Override
    public void sendFinalStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId) {
        playGameService.setState(playGameService.createStateAfterMoveDown((State) playGameService.getState(userId), service, gameService), userId);
        char[][] cellsToBeDisplayed = playGameService.getState(userId).getStage().drawTetraminoOnCells();
        State stateToBeSent = playGameService.getState(userId).buildState(playGameService.getState(userId).getStage().buildStage(cellsToBeDisplayed), playGameService.getState(userId).isRunning(), playGameService.getState(userId).getGame());
        template.convertAndSend("/receive/stateFinal", stateToBeSent);
    }

    @Override
    public void sendSavedStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String userId) {
        playGameService.setState(playGameService.createStateAfterMoveDown((State) playGameService.getState(userId), service, gameService), userId);
        char[][] cellsToBeDisplayed = playGameService.getState(userId).getStage().drawTetraminoOnCells();
        State stateToBeSent = playGameService.getState(userId).buildState(playGameService.getState(userId).getStage().buildStage(cellsToBeDisplayed), playGameService.getState(userId).isRunning(), playGameService.getState(userId).getGame());
        template.convertAndSend("/receive/stateSaved", stateToBeSent);
    }

    @Override
    public void sendDaoGameToBeDisplayed(Game game, SimpMessagingTemplate template) {
        template.convertAndSend("/receive/daoGameObjects", game);
    }

    @Override
    public void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template) {
        template.convertAndSend("/receive/gameObjects", game);
    }
}
