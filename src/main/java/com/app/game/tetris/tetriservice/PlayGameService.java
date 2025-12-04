package com.app.game.tetris.tetriservice;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.model.Stage;
import com.app.game.tetris.model.State;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public interface PlayGameService {
    void setState(State state, String userId);

    State getState(String userId);

    State createStateAfterMoveDown(State state, ScheduledExecutorService service, GameService gameService, String userId);

    Game createGame(String playerName, int playerScore);

    State initiateState(String playerName, String userId);

    State dropDownState(State state, String userId);

    State moveRightState(State state, String userId);

    State moveLeftState(State state, String userId);

    State rotateState(State state, String userId);

    Optional<State> moveDownState(State state);

    Optional<State> newTetraminoState(State state);

    SavedGame saveGame(Game game, State state);

    State recreateStateFromSavedGame(SavedGame savedGame, String userId);

    ScheduledExecutorService getSEService(String userId);

    void setSEService(ScheduledExecutorService service, String userId);

    State buildState(Stage stage, boolean isRunning, Game game);

    Stage buildStage(char[][] cells, Tetramino tetramino, int tetraminoX, int tetraminoY, int collapsedLayersCount);

    Stage buildStage(char[][] cells, State state);

    char[][] drawTetraminoOnCells(State state);
}
