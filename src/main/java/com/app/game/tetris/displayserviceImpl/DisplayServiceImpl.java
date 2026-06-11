package com.app.game.tetris.displayserviceImpl;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.dto.GameDataDTO;
import com.app.game.tetris.dto.GameStateDTO;
import com.app.game.tetris.dto.PlayerHelloDTO;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.State;
import com.app.game.tetris.tetriservice.PlayGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DisplayServiceImpl implements DisplayService {

    @Override
    public void sendStateToBeDisplayed(PlayGameService playGameService, GameService gameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        // 🔥 ИСПРАВЛЕНИЕ RACE CONDITION: Извлекаем актуальное состояние,
        // но НЕ мутируем его и не вызываем setState здесь! Это задача PlayGameService.
        State state = playGameService.getState(userId);

        if (state == null) {
            log.warn("⚠️ [Display] Попытка отрисовать несуществующее состояние для пользователя ID: {}", userId);
            return;
        }

        // Формируем визуал на основе стабильного кадра
        char[][] cells = playGameService.drawTetraminoOnCells(state);

        GameStateDTO stateRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                state.isRunning(),
                cells
        );

        template.convertAndSendToUser(destinationId, "/queue/stateObjects", stateRecord);
    }

    @Override
    public void sendFinalStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        State state = playGameService.getState(userId);

        // 🔥 ЗАЩИТА ОТ NULL: Предотвращаем краш потока Loom при Game Over
        if (state == null) {
            log.warn("⚠️ [Display] Финальный кадр запрошен, но стейт для ID: {} уже удален", userId);
            return;
        }

        char[][] cells = playGameService.drawTetraminoOnCells(state);

        GameStateDTO finalRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                false, // Явно форсируем false для финала
                cells
        );

        template.convertAndSendToUser(destinationId, "/queue/stateFinal", finalRecord);
    }

    @Override
    public void sendSavedStateToBeDisplayed(PlayGameService playGameService, SimpMessagingTemplate template, String destinationId) {
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        var state = playGameService.getState(userId);
        if (state == null) {
            log.warn("⚠️ [Display] Попытка сохранить визуализацию для несуществующего ID: {}", userId);
            return;
        }

        char[][] cells = playGameService.drawTetraminoOnCells(state);

        GameStateDTO savedRecord = new GameStateDTO(
                state.getGame().getPlayerName(),
                state.getGame().getPlayerScore(),
                state.isRunning(),
                cells
        );

        template.convertAndSendToUser(destinationId, "/queue/stateSaved", savedRecord);
    }

    @Override
    public void sendDaoGameToBeDisplayed(GameDataDTO gameData, SimpMessagingTemplate template, String destinationId) {
        // 🔥 ЗАЩИТА ОТ PINNING: Заменяем System.out на асинхронный логгер через {}
        log.debug("📡 Отправка GameDataDTO для {}. Данные: {}", destinationId, gameData);
        try {
            template.convertAndSend("/topic/daoGameObjects", gameData);
        } catch (Exception e) {
            log.error("💥 КРИТИКА: Сбой отправки WebSocket сообщения в общую тему для {}: {}", destinationId, e.getMessage());
        }
    }

    @Override
    public void sendGameToBeDisplayed(Game game, SimpMessagingTemplate template, String userId) {
        if (game == null) return;
        PlayerHelloDTO helloRecord = new PlayerHelloDTO(game.getPlayerName());
        template.convertAndSendToUser(userId, "/queue/gameObjects", helloRecord);
    }
}
