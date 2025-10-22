package com.app.game.tetris.tetriservice;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.tetriserviceImpl.State;

import java.util.Optional;

public interface PlayGameService {


    void setState(StateService state);

    StateService getState();

    Game createGame(String playerName, int playerScore);
    StateService initiateState(String playerName);
    StateService dropDownState(State state);
    StateService moveRightState(State state);
    StateService moveLeftState(State state);
    StateService rotateState(State state);
    Optional<State> moveDownState(State state);
    Optional<State> newTetraminoState(State state);
    SavedGame saveGame(Game game, State state);
    State recreateStateFromSavedGame(SavedGame savedGame);
}
