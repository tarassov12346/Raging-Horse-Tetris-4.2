package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.client.GameClient;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GameServiceImpl implements GameService {
    private final GameClient gameClient;

    // Рекомендую инжекцию через конструктор вместо @Autowired на поле
    public GameServiceImpl(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    @Override
    public String getGameData(String playerName) {
        return gameClient.getGameData(playerName);
    }
    @Override
    public List<Game> getAllGames() {
        // Feign сам преобразует JSON в List<Game>, стримы больше не нужны
        return gameClient.getAllGames();
    }
    @Override
    public void deleteGameData(String playerName) {
        gameClient.deleteGameData(playerName);
    }
    @Override
    public void doRecord(Game game) {
        gameClient.doRecord(game);
    }
    @Override
    public Set<Game> getAllBestResults(List<Game> playersList) {
        // Локальная логика обработки остается без изменений
        Set<Game> highestScoringPlayers = new HashSet<>(playersList);
        return highestScoringPlayers;
    }
}
