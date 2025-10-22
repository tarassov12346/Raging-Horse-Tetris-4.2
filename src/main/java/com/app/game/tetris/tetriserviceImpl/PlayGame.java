package com.app.game.tetris.tetriserviceImpl;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.tetriservice.GameLogic;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.tetriservice.StageService;
import com.app.game.tetris.tetriservice.StateService;
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

    @Override
    public void setState(StateService state) {
        this.state = state;
    }

    @Override
    public StateService getState() {
        return state;
    }

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
    public StateService initiateState(String playerName) {
        Stage emptyStage = stage.buildStage(makeEmptyMatrix(), getTetramino0(), 0, 0, 0);
        State initialState = state.buildState(emptyStage, false, createGame(playerName,0));

        state=initialState.start().createStateWithNewTetramino().orElse(initialState);

        return state;


    }

    @Override
    public StateService dropDownState(State state) {
        this.state=state.dropDown().orElse(state);

        return this.state;

    }

    @Override
    public StateService moveRightState(State state) {
        this.state=state.moveRight().orElse(state);

        return this.state;

    }

    @Override
    public StateService moveLeftState(State state) {
        this.state=state.moveLeft().orElse(state);

        return this.state;

    }

    @Override
    public StateService rotateState(State state) {
        this.state=state.rotate().orElse(state);

        return this.state;

    }

    @Override
    public Optional<State> moveDownState(State state) {
  //      this.state=state.moveDown(state.getStepDown()).orElse(state);
        return state.moveDown(state.getStepDown());}

    @Override
    public Optional<State> newTetraminoState(State state) {
  //      this.state=state.createStateWithNewTetramino().orElse(state);
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
        state=state.buildState(recreatedStage, true, recreatedGame).restartWithNewTetramino().orElse(state.buildState(recreatedStage, true, recreatedGame));
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
