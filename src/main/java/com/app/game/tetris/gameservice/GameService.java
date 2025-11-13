package com.app.game.tetris.gameservice;

import com.app.game.tetris.model.Game;

import java.util.List;
import java.util.Set;

public interface GameService {
    String getGameData(String playerName);
    List<Game> getAllGames();
    void deleteGameData(String playerName);
    void doRecord(Game game);
    Set<Game> getAllBestResults(List<Game> playersList );
}
