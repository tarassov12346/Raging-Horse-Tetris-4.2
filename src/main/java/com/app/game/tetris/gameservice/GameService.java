package com.app.game.tetris.gameservice;

import com.app.game.tetris.model.Game;

import java.util.List;

public interface GameService {
    String getGameData(String playerName);

    List<Game> getAllGames();

    void deleteGameData(String playerName);

    void doRecord(Game game);
}
