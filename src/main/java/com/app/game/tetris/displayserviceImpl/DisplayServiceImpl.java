package com.app.game.tetris.displayserviceImpl;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.model.State;
import com.google.gson.Gson;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;

@Service
public class DisplayServiceImpl implements DisplayService {

    @Override
    public void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        // Обновляем состояние
        State state = playGameService.createStateAfterMoveDown(playGameService.getState(userId), service, gameService, userId);
        playGameService.setState(state, userId);
        // Формируем визуал
        char[][] cells = playGameService.drawTetraminoOnCells(state);
        State stateToBeSent = playGameService.buildState(playGameService.buildStage(cells, state), state.isRunning(), state.getGame());
        // ВАЖНО: Отправляем ТОЛЬКО конкретному пользователю
        template.convertAndSendToUser(destinationId, "/queue/stateObjects", stateToBeSent);
    }

    @Override
    public void sendFinalStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        State state = playGameService.getState(userId);
        char[][] cells = playGameService.drawTetraminoOnCells(state);
        State stateToBeSent = playGameService.buildState(playGameService.buildStage(cells, state), state.isRunning(), state.getGame());

        template.convertAndSendToUser(destinationId, "/queue/stateFinal", stateToBeSent);
    }

    @Override
    public void sendSavedStateToBeDisplayed(PlayGameService playGameService, GameService gameService, ScheduledExecutorService service, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        State state = playGameService.getState(userId);
        char[][] cells = playGameService.drawTetraminoOnCells(state);
        State stateToBeSent = playGameService.buildState(playGameService.buildStage(cells, state), state.isRunning(), state.getGame());

        template.convertAndSendToUser(destinationId, "/queue/stateSaved", stateToBeSent);
    }

    @Override
    public void sendDaoGameToBeDisplayed(Game game, SimpMessagingTemplate template, String destinationId) {
        System.out.println("DEBUG: Sending DAO objects to " + destinationId);
        Gson gson = new Gson();
        System.out.println(gson.toJson(game));
        // Отправляем конкретному юзеру его (или общие) рекорды
        try {
            template.convertAndSend("/topic/daoGameObjects", game);
        } catch (Exception e) {
            e.printStackTrace(); // Если тут будет ошибка, ты увидишь её в IDE
        }

    }

    @Override
    public void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template, String userId) {
        // Отправляем данные только игроку, который вошел
        template.convertAndSendToUser(userId, "/queue/gameObjects", game);
    }

}
