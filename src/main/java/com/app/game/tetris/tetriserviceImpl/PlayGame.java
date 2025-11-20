package com.app.game.tetris.tetriserviceImpl;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.tetriservice.GameLogic;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.tetriservice.StageService;
import com.app.game.tetris.tetriservice.StateService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

@Service
public class PlayGame implements PlayGameService {
    private final Game game;
    private final StageService stage;
    private final StateService state;
    private final Tetramino tetramino;
    private final SavedGame savedGame;

    private final ConcurrentHashMap<String, StateService> userStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledExecutorService> userExecutors = new ConcurrentHashMap<>();

    public PlayGame(Game game, StageService stage, StateService state, Tetramino tetramino, SavedGame savedGame) {
        this.game = game;
        this.stage = stage;
        this.state = state;
        this.tetramino = tetramino;
        this.savedGame = savedGame;
    }

    public void removeStateForUser(String userId) {
        userStates.remove(userId);
        ScheduledExecutorService executor = userExecutors.remove(userId);
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public ScheduledExecutorService getSEService(String userId) {
        return userExecutors.get(userId);
    }

    @Override
    public void setSEService(ScheduledExecutorService service, String userId) {
        userExecutors.put(userId, service);
    }

    @Override
    public void setState(StateService state, String userId) {
        userStates.put(userId, state);
    }

    @Override
    public StateService getState(String userId) {
        return userStates.get(userId);
    }

    @Override
    public State createStateAfterMoveDown(State state, ScheduledExecutorService service, GameService gameService) {
        Optional<State> moveDownState = moveDownState(state);
        if (moveDownState.isEmpty()) {
            Optional<State> newTetraminoState = newTetraminoState(state);
            if (newTetraminoState.isEmpty()) {
                state = state.stop();
                if (!service.isShutdown()) gameService.doRecord(state.getGame());
                service.shutdown();
                return state;
            } else state = newTetraminoState.orElse(state);
        }
        state = moveDownState.orElse(state);
        return state;
    }

    @Override
    public Game createGame(String playerName, int playerScore) {
        return game.buildGame(playerName, playerScore);
    }

    @Override
    public StateService initiateState(String playerName, String userId) {
        Stage emptyStage = stage.buildStage(makeEmptyMatrix(), getTetramino0(), 0, 0, 0);
        State initialState = state.buildState(emptyStage, false, createGame(playerName, 0));
        userStates.put(userId, initialState.start().createStateWithNewTetramino().orElse(initialState));
        setState(userStates.get(userId), userId);
        return getState(userId);
    }

    @Override
    public StateService dropDownState(State state, String userId) {
        setState(state.dropDown().orElse(state), userId);
        return getState(userId);
    }

    @Override
    public StateService moveRightState(State state, String userId) {
        setState(state.moveRight().orElse(state), userId);
        return getState(userId);
    }

    @Override
    public StateService moveLeftState(State state, String userId) {
        setState(state.moveLeft().orElse(state), userId);
        return getState(userId);
    }

    @Override
    public StateService rotateState(State state, String userId) {
        setState(state.rotate().orElse(state), userId);
        return getState(userId);
    }

    @Override
    public Optional<State> moveDownState(State state) {
        return state.moveDown(state.getStepDown());
    }

    @Override
    public Optional<State> newTetraminoState(State state) {
        return state.createStateWithNewTetramino();
    }

    @Override
    public SavedGame saveGame(Game game, State state) {
        return savedGame.buildSavedGame(game.getPlayerName(), game.getPlayerScore(), state.getStage().getCells());
    }

    @Override
    public State recreateStateFromSavedGame(SavedGame savedGame, String userId) {
        Game recreatedGame = game.buildGame(savedGame.getPlayerName(), savedGame.getPlayerScore());
        Stage recreatedStage = stage.buildStage(savedGame.getCells(), getTetramino0(), 0, 0, recreatedGame.getPlayerScore() / 10);
        setState(state.buildState(recreatedStage, true, recreatedGame).restartWithNewTetramino().orElse(state.buildState(recreatedStage, true, recreatedGame)), userId);
        return (State) getState(userId);
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
