package com.app.game.tetris.serviceImpl;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.service.GameLogic;
import com.app.game.tetris.service.PlayGameService;
import com.app.game.tetris.service.StageService;
import com.app.game.tetris.service.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class PlayGame implements PlayGameService {

    @Autowired
    private Game game;

    @Autowired
    private StageService stage;

    @Autowired
    private StateService state;

    @Autowired
    private Tetramino tetramino;

    @Autowired
    private SavedGame savedGame;

    @Override
    public Game createGame(String playerName, int playerScore) {
        return game.buildGame(playerName, playerScore);
    }

    @Override
    public State initiateState(String playerName) {
        Stage emptyStage = stage.buildStage(makeEmptyMatrix(), getTetramino0(), 0, 0, 0);
        State initialState = state.buildState(emptyStage, false, createGame(playerName,0));
        return initialState.start().createStateWithNewTetramino().orElse(initialState);
    }

    @Override
    public State dropDownState(State state) {return state.dropDown().orElse(state);
    }

    @Override
    public State moveRightState(State state) {
        return state.moveRight().orElse(state);
    }

    @Override
    public State moveLeftState(State state) {
        return state.moveLeft().orElse(state);
    }

    @Override
    public State rotateState(State state) {
        return state.rotate().orElse(state);
    }

    @Override
    public Optional<State> moveDownState(State state) {return state.moveDown(state.getStepDown());}

    @Override
    public Optional<State> newTetraminoState(State state) {
        return state.createStateWithNewTetramino();
    }

    @Override
    public SavedGame saveGame(Game game, State state) {
        return savedGame.buildSavedGame(game.getPlayerName(), game.getPlayerScore(), state.getStage().getCells());
    }

    @Override
    public State recreateStateFromSavedGame(SavedGame savedGame) {
        Game recreatedGame = game.buildGame(savedGame.getPlayerName(), savedGame.getPlayerScore());
        Stage recreatedStage = stage.buildStage(savedGame.getCells(), getTetramino0(), 0, 0, recreatedGame.getPlayerScore() / 10);
        return state.buildState(recreatedStage, true, recreatedGame).restartWithNewTetramino().orElse(state.buildState(recreatedStage, true, recreatedGame));
    }

    private char[][] makeEmptyMatrix() {
        final char[][] c = new char[GameLogic.HEIGHT][GameLogic.WIDTH];
        IntStream.range(0, GameLogic.HEIGHT).forEach(y -> IntStream.range(0, GameLogic.WIDTH).forEach(x -> c[y][x] = '0'));
        return c;
    }

    private Tetramino getTetramino0() {
        return tetramino.buildTetramino(new char[][]{{'0'}});
    }
}
