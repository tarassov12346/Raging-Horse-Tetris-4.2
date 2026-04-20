package com.app.game.tetris.displayserviceImpl;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.dto.GameDataDTO;
import com.app.game.tetris.dto.GameStateDTO;
import com.app.game.tetris.dto.PlayerHelloDTO;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.State;
import com.app.game.tetris.tetriservice.PlayGameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class DisplayServiceImpl implements DisplayService {

    @Override
    public void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        // Обновляем состояние
        State state = playGameService.createStateAfterMoveDown(playGameService.getState(userId), gameService, userId);
        if (state == null) {
            System.out.println("Попытка отрисовать несуществующее состояние для юзера: " + userId);
            return; // Просто выходим, не роняя приложение
        }
        playGameService.setState(state, userId);
        // Формируем визуал
        char[][] cells = playGameService.drawTetraminoOnCells(state);
        // 3. Создаем рекорд СРАЗУ с нужными полями
        GameStateDTO stateRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                state.isRunning(),
                cells
        );
        // 4. Отправляем рекорд напрямую пользователю
        template.convertAndSendToUser(destinationId, "/queue/stateObjects", stateRecord);
    }

    @Override
    public void sendFinalStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        // Получаем текущее состояние
        State state = playGameService.getState(userId);
        // Формируем финальный визуал
        char[][] cells = playGameService.drawTetraminoOnCells(state);
        // Создаем рекорд GameStateDTO (тот же, что и для обычного стейта)
        GameStateDTO finalRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                state.isRunning(), // Здесь обычно будет false, так как это финал
                cells
        );
        // Отправляем рекорд в топик финального состояния
        template.convertAndSendToUser(destinationId, "/queue/stateFinal", finalRecord);
    }

    @Override
    public void sendSavedStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        // 1. Получаем текущее состояние из памяти
        var state = playGameService.getState(userId);
        if (state == null) {
            System.out.println("Попытка отрисовать несуществующее состояние для юзера: " + userId);
            return; // Просто выходим, не роняя приложение
        }
        // 2. Генерируем матрицу (визуал)
        char[][] cells = playGameService.drawTetraminoOnCells(state);

        // 3. Создаем рекорд GameStateDTO вместо тяжелого State
        GameStateDTO savedRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                state.isRunning(),
                cells
        );
        // 4. Отправляем компактный рекорд напрямую пользователю
        template.convertAndSendToUser(destinationId, "/queue/stateSaved", savedRecord);
    }


    @Override
    public void sendDaoGameToBeDisplayed(GameDataDTO gameData, SimpMessagingTemplate template, String destinationId) {
        // Рекорды в Java 21 имеют отличный встроенный toString() для логирования
        System.out.println("DEBUG: Sending GameDataDTO to " + destinationId);
        System.out.println("Payload: " + gameData);
        try {
            // Spring Boot 3.4 через Jackson автоматически сериализует рекорд в JSON
            template.convertAndSend("/topic/daoGameObjects", gameData);
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to send WebSocket message to " + destinationId);
            e.printStackTrace();
        }
    }

    @Override
    public void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template, String userId) {
        // Создаем легкий рекорд, беря только имя игрока
        PlayerHelloDTO helloRecord = new PlayerHelloDTO(game.getPlayerName());
        // Отправляем ТОЛЬКО имя (в формате JSON { "playerName": "..." })
        template.convertAndSendToUser(userId, "/queue/gameObjects", helloRecord);
    }
}
