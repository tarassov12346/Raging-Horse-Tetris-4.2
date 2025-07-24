package com.app.game.tetris.service;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.serviceImpl.State;

import java.util.Optional;

public interface PlayGameService {


    Game createGame(String playerName, int playerScore);
    State initiateState(String playerName);
    State dropDownState(State state);
    State moveRightState(State state);
    State moveLeftState(State state);
    State rotateState(State state);
    Optional<State> moveDownState(State state);
    Optional<State> newTetraminoState(State state);
    SavedGame saveGame(Game game, State state);
    State recreateStateFromSavedGame(SavedGame savedGame);
}
