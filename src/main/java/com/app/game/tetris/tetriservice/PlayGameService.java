package com.app.game.tetris.tetriservice;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.*;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public interface PlayGameService {
    void setState(State state, String userId);

    State getState(String userId);

    State createStateAfterMoveDown(State state, GameService gameService, String userId);

    Game createGame(String playerName, int playerScore);

    State initiateState(String playerName, String userId);

    // 1. Добавляем метод проверки активности таймера
    boolean hasUserTask(String userId);

    // 2. Изменяем сигнатуры методов движения (убираем передачу State снаружи)
    State rotateState(String userId);
    State moveLeftState(String userId);
    State moveRightState(String userId);
    State dropDownState(String userId);


    Optional<State> moveDownState(State state);

    Optional<State> newTetraminoState(State state);

    SavedGame saveGame(Game game, State state);

    State recreateStateFromSavedGame(SavedGame savedGame, String userId);

    void setUserTask(String userId, ScheduledFuture<?> task);

    void stopUserTask(String userId);
    State buildState(Stage stage, boolean isRunning, Game game);

    Stage buildStage(char[][] cells, Tetramino tetramino, int tetraminoX, int tetraminoY, int collapsedLayersCount);

    Stage buildStage(char[][] cells, State state);

    void removeStateForUser(String userId);

    char[][] drawTetraminoOnCells(State state);
}
