package com.app.game.tetris.tetriserviceImpl;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.*;
import com.app.game.tetris.tetriservice.PlayGameService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

@Service
public class PlayGame implements PlayGameService {
    int WIDTH = 12;
    int HEIGHT = 20;
    private final ConcurrentHashMap<String, State> userStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledExecutorService> userExecutors = new ConcurrentHashMap<>();

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
    public void setState(State state, String userId) {
        userStates.put(userId, state);
    }

    @Override
    public State getState(String userId) {
        return userStates.get(userId);
    }

    @Override
    public State createStateAfterMoveDown(State state, ScheduledExecutorService service, GameService gameService, String userId) {
        Optional<State> moveDownState = moveDownState(state);
        if (moveDownState.isEmpty()) {
            Optional<State> newTetraminoState = newTetraminoState(state);
            if (newTetraminoState.isEmpty()) {
                state = buildState(state.getStage(), false, state.getGame());
                if (!service.isShutdown()) gameService.doRecord(state.getGame());
                service.shutdown();
                setState(state, userId);
                return getState(userId);
            } else state = newTetraminoState.orElse(state);
        }
        state = moveDownState.orElse(state);
        setState(state, userId);
        return getState(userId);
    }

    @Override
    public Game createGame(String playerName, int playerScore) {
        return buildGame(playerName, playerScore);
    }

    @Override
    public State initiateState(String playerName, String userId) {
        Stage emptyStage = buildStage(makeEmptyMatrix(), getTetramino0(), 0, 0, 0);
        State initialState = buildState(emptyStage, true, createGame(playerName, 0));
        userStates.put(userId, createStateWithNewTetramino(initialState).orElse(initialState));
        setState(userStates.get(userId), userId);
        return getState(userId);
    }

    @Override
    public State dropDownState(State state, String userId) {
        setState(dropDown(state).orElse(state), userId);
        return getState(userId);
    }

    @Override
    public State moveRightState(State state, String userId) {
        setState(moveRight(state).orElse(state), userId);
        return getState(userId);
    }

    @Override
    public State moveLeftState(State state, String userId) {
        setState(moveLeft(state).orElse(state), userId);
        return getState(userId);
    }

    @Override
    public State rotateState(State state, String userId) {
        setState(rotate(state).orElse(state), userId);
        return getState(userId);
    }

    @Override
    public Optional<State> moveDownState(State state) {
        return moveDown(state, state.getStepDown());
    }

    @Override
    public Optional<State> newTetraminoState(State state) {
        return createStateWithNewTetramino(state);
    }

    @Override
    public SavedGame saveGame(Game game, State state) {
        return buildSavedGame(game.getPlayerName(), game.getPlayerScore(), state.getStage().getCells());
    }

    @Override
    public State recreateStateFromSavedGame(SavedGame savedGame, String userId) {
        Game recreatedGame = buildGame(savedGame.getPlayerName(), savedGame.getPlayerScore());
        Stage recreatedStage = buildStage(savedGame.getCells(), getTetramino0(), 0, 0, recreatedGame.getPlayerScore() / 10);
        setState(restartWithNewTetramino(buildState(recreatedStage, true, recreatedGame)).orElse(buildState(recreatedStage, true, recreatedGame)), userId);
        return getState(userId);
    }

    @Override
    public State buildState(Stage stage, boolean isRunning, Game game) {
        return new State(stage, isRunning, game);
    }

    @Override
    public Stage buildStage(char[][] cells, Tetramino tetramino, int tetraminoX, int tetraminoY, int collapsedLayersCount) {
        return new Stage(cells, tetramino, tetraminoX, tetraminoY, collapsedLayersCount);
    }

    @Override
    public Stage buildStage(char[][] cells, State state) {
        return buildStage(cells, state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount());
    }

    @Override
    public char[][] drawTetraminoOnCells(State state) {
        //  final char[][] c = Arrays.stream(cells).map(char[]::clone).toArray(char[][]::new); // copy
        final char[][] c = Arrays.stream(state.getStage().getCells()).map(char[]::clone).toArray(char[][]::new); // copy
        IntStream.range(0, state.getStage().getTetramino().getShape().length).forEach(y ->
                IntStream.range(0, state.getStage().getTetramino().getShape()[0].length).forEach(x -> {
                    if (state.getStage().getTetramino().getShape()[y][x] != '0')
                        c[state.getStage().getTetraminoY() + y][state.getStage().getTetraminoX() + x] = state.getStage().getTetramino().getShape()[y][x];
                }));
        return c;
    }

    private char[][] makeEmptyMatrix() {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));
        return c;
    }

    private Tetramino getTetramino0() {
        return buildTetramino(new char[][]{{'0'}});
    }

    private Tetramino buildTetramino(char[][] shape) {
        return new Tetramino(shape);
    }

    private Optional<State> moveLeft(State state) {
        return !checkCollision(state, -1, 0, false) ? Optional.of(moveTetraminoLeft(state)) : Optional.empty();
    }

    private Optional<State> moveRight(State state) {
        return !checkCollision(state, 1, 0, false) ? Optional.of(moveTetraminoRight(state)) : Optional.empty();
    }

    private Optional<State> moveDown(State state, int step) {
        int yToStepDown;
        for (yToStepDown = 0; (yToStepDown <= step) && (yToStepDown < HEIGHT); yToStepDown++) {
            if (checkCollision(state, 0, yToStepDown, false)) break;
        }
        return !checkCollision(state, 0, 1, false) ? Optional.of(moveTetraminoDown(state, yToStepDown - 1)) : Optional.empty();
    }

    public Optional<State> rotate(State state) {
        return !checkCollision(state, 0, 0, true) ? Optional.of(rotateTetramino(state)) : Optional.empty();
    }

    public Optional<State> dropDown(State state) {
        int yToDropDown;
        for (yToDropDown = 0; yToDropDown < HEIGHT; yToDropDown++) {
            if (checkCollision(state, 0, yToDropDown, false)) break;
        }
        return !checkCollision(state, 0, yToDropDown - 1, false) ? Optional.of(moveTetraminoDown(state, yToDropDown - 1)) : Optional.empty();
    }

    private State moveTetraminoLeft(State state) {
        return buildState(buildStage(state.getStage().getCells(), state.getStage().getTetramino(), state.getStage().getTetraminoX() - 1, state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame());
    }

    private State moveTetraminoRight(State state) {
        return buildState(buildStage(state.getStage().getCells(), state.getStage().getTetramino(), state.getStage().getTetraminoX() + 1, state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame());
    }

    private State moveTetraminoDown(State state, int yToMoveDown) {
        return buildState(buildStage(state.getStage().getCells(), state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY() + yToMoveDown, state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame());
    }

    private State rotateTetramino(State state) {
        return buildState(buildStage(state.getStage().getCells(), buildTetramino(rotateMatrix(state.getStage().getTetramino().getShape())), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame());
    }

    private Optional<State> initiateTetramino(Tetramino tetramino, int x, int y, State state) {
        return Optional.of(buildState(buildStage(state.getStage().getCells(), tetramino, x, y, state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame()));
    }

    private Optional<State> createStateWithNewTetramino(State state) {
        final Tetramino t = getRandomTetramino(state);
        State newState = burryTetramino(state).orElse(state);
        newState = buildState(collapseFilledLayers(newState), newState.isRunning(), newState.getGame());
        newState = updatePlayerScore(newState);
        newState = initiateTetramino(t, (WIDTH - t.getShape().length) / 2, 0, newState).orElse(newState);
        return !checkCollision(newState, 0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    private Optional<State> restartWithNewTetramino(State state) {
        final Tetramino t = getRandomTetramino(state);
        State newState = burryTetramino(state).orElse(state);
        newState = initiateTetramino(t, (WIDTH - t.getShape().length) / 2, 0, newState).orElse(newState);
        return !checkCollision(newState, 0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    private Tetramino getRandomTetramino(State state) {
        final Map<Character, Tetramino> tetraminoMap = new HashMap<>();
        tetraminoMap.put('0', state.getStage().getTetramino().buildTetramino(new char[][]{{'0'}}));
        tetraminoMap.put('I', state.getStage().getTetramino().buildTetramino(new char[][]{{'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}}));
        tetraminoMap.put('J', state.getStage().getTetramino().buildTetramino(new char[][]{{'0', 'J', '0'}, {'0', 'J', '0'}, {'J', 'J', '0'}}));
        tetraminoMap.put('L', state.getStage().getTetramino().buildTetramino(new char[][]{{'0', 'L', '0'}, {'0', 'L', '0'}, {'0', 'L', 'L'}}));
        tetraminoMap.put('O', state.getStage().getTetramino().buildTetramino(new char[][]{{'O', 'O'}, {'O', 'O'}}));
        tetraminoMap.put('S', state.getStage().getTetramino().buildTetramino(new char[][]{{'0', 'S', 'S'}, {'S', 'S', '0'}, {'0', '0', '0'}}));
        tetraminoMap.put('T', state.getStage().getTetramino().buildTetramino(new char[][]{{'0', '0', '0'}, {'T', 'T', 'T'}, {'0', 'T', '0'}}));
        tetraminoMap.put('Z', state.getStage().getTetramino().buildTetramino(new char[][]{{'Z', 'Z', '0'}, {'0', 'Z', 'Z'}, {'0', '0', '0'}}));
        tetraminoMap.put('K', state.getStage().getTetramino().buildTetramino(new char[][]{{'K', 'K', 'K'}, {'0', 'K', '0'}, {'0', 'K', '0'}}));
        final char[] tetraminos = "IJLOSTZK".toCharArray();
        return tetraminoMap.get(tetraminos[new Random().nextInt(tetraminos.length)]);
    }

    public Optional<State> burryTetramino(State state) {
        return Optional.of(buildState(buildStage(drawTetraminoOnCells(state), state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame()));
    }

    private State updatePlayerScore(State state) {

        return new State(collapseFilledLayers(state), state.isRunning(), buildGame(state.getGame().getPlayerName(), state.getStage().getCollapsedLayersCount() * 10));
    }

    private Stage collapseFilledLayers(State state) {
        final char[][] c = Arrays.stream(state.getStage().getCells()).map(char[]::clone).toArray(char[][]::new); // copy
        final int[] ny2 = {0, HEIGHT - 1};

        IntStream.rangeClosed(0, HEIGHT - 1).forEach(y1 -> {
            if (!isFull(state.getStage().getCells()[HEIGHT - 1 - y1])) {
                System.arraycopy(c, HEIGHT - 1 - y1, c, ny2[1]--, 1);
            } else {
                ny2[0]++;
            }
        });
        return buildStage(c, state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount() + ny2[0]);
    }

    private boolean isFull(char[] row) {
        return IntStream.range(0, row.length).noneMatch(i -> row[i] == '0');
    }

    private boolean checkCollision(State state, int dx, int dy, boolean rotate) {
        final char[][] m = rotate ? rotateMatrix(state.getStage().getTetramino().getShape()) : state.getStage().getTetramino().getShape();
        final int h = m.length;
        final int w = m[0].length;
        return IntStream.range(0, h).anyMatch(y -> IntStream.range(0, w).anyMatch(x -> (
                m[y][x] != '0' && ((state.getStage().getTetraminoY() + y + dy >= HEIGHT)
                        || ((state.getStage().getTetraminoX() + x + dx) < 0)
                        || ((state.getStage().getTetraminoX() + x + dx) >= WIDTH)
                        || (state.getStage().getCells()[state.getStage().getTetraminoY() + y + dy][state.getStage().getTetraminoX() + x + dx] != '0'))
        )));
    }

    private char[][] rotateMatrix(char[][] m) {
        final int h = m.length;
        final int w = m[0].length;
        final char[][] t = new char[h][w];
        IntStream.range(0, h).forEach(y -> IntStream.range(0, w).forEach(x -> t[w - x - 1][y] = m[y][x]));
        return t;
    }

    private Game buildGame(String playerName, int playerScore) {
        return new Game(playerName, playerScore);
    }

    private SavedGame buildSavedGame(String playerName, int playerScore, char[][] cells) {
        return new SavedGame(playerName, playerScore, cells);
    }
}
