package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.client.GameClient;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GameServiceImpl implements GameService {
    private final GameClient gameClient;

    public GameServiceImpl(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    @Async
    @Override
    public CompletableFuture<String> getGameData(String playerName) {
        log.info("🌐 Запрос рекордов для {} через виртуальный поток", playerName);
        String data = gameClient.getGameData(playerName);
        return CompletableFuture.completedFuture(data);
    }

    @Override
    public List<Game> getAllGames() {
        // Оставляем синхронным, если этот список нужен немедленно для дальнейшей логики
        return gameClient.getAllGames();
    }

    @Async
    @Override
    public void deleteGameData(String playerName) {
        gameClient.deleteGameData(playerName);
    }

    @Async
    @Override
    public void doRecord(Game game) {
        log.info("🏆 Сохранение рекорда в фоне...");
        gameClient.doRecord(game);
    }
}

