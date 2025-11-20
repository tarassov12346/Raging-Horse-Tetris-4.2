package com.app.game.tetris.tetriservice;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.tetriserviceImpl.State;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public interface PlayGameService {
    void setState(StateService state, String userId);

    StateService getState(String userId);

    State createStateAfterMoveDown(State state, ScheduledExecutorService service, GameService gameService);

    Game createGame(String playerName, int playerScore);

    StateService initiateState(String playerName, String userId);

    StateService dropDownState(State state, String userId);

    StateService moveRightState(State state, String userId);

    StateService moveLeftState(State state, String userId);

    StateService rotateState(State state, String userId);

    Optional<State> moveDownState(State state);

    Optional<State> newTetraminoState(State state);

    SavedGame saveGame(Game game, State state);

    State recreateStateFromSavedGame(SavedGame savedGame, String userId);

    ScheduledExecutorService getSEService(String userId);

    void setSEService(ScheduledExecutorService service, String userId);
}
