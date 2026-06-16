package com.app.game.tetris.tetriserviceImpl;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.*;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.IntStream;



@Service
public class PlayGame implements PlayGameService {
    private static final Logger log = LoggerFactory.getLogger(PlayGame.class);

    // 🔥 ОПТИМИЗАЦИЯ ПАМЯТИ: Статический пул, чтобы не плодить HashMap на каждый спавн фигуры
    private static final Map<Character, Tetramino> TETRAMINO_POOL = new HashMap<>();
    private static final char[] TETRAMINO_KEYS = "IJLOSTZK".toCharArray();
    private static final Random RANDOM = new Random();

    static {
        TETRAMINO_POOL.put('0', new Tetramino(new char[][]{{'0'}}));
        TETRAMINO_POOL.put('I', new Tetramino(new char[][]{{'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}}));
        TETRAMINO_POOL.put('J', new Tetramino(new char[][]{{'0', 'J', '0'}, {'0', 'J', '0'}, {'J', 'J', '0'}}));
        TETRAMINO_POOL.put('L', new Tetramino(new char[][]{{'0', 'L', '0'}, {'0', 'L', '0'}, {'0', 'L', 'L'}}));
        TETRAMINO_POOL.put('O', new Tetramino(new char[][]{{'O', 'O'}, {'O', 'O'}}));
        TETRAMINO_POOL.put('S', new Tetramino(new char[][]{{'0', 'S', 'S'}, {'S', 'S', '0'}, {'0', '0', '0'}}));
        TETRAMINO_POOL.put('T', new Tetramino(new char[][]{{'0', '0', '0'}, {'T', 'T', 'T'}, {'0', 'T', '0'}}));
        TETRAMINO_POOL.put('Z', new Tetramino(new char[][]{{'Z', 'Z', '0'}, {'0', 'Z', 'Z'}, {'0', '0', '0'}}));
        TETRAMINO_POOL.put('K', new Tetramino(new char[][]{{'K', 'K', 'K'}, {'0', 'K', '0'}, {'0', 'K', '0'}}));
    }


    @Value("${width}")
    int WIDTH;
    @Value("${height}")
    int HEIGHT;

    private final TaskScheduler taskScheduler;
    private final IMap<String, State> userStates;

    public PlayGame(TaskScheduler taskScheduler, HazelcastInstance hazelcastInstance) {
        this.taskScheduler = taskScheduler;
        this.userStates = hazelcastInstance.getMap("user-states");
    }

    private final ConcurrentHashMap<String, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();

    @Override
    public void setUserTask(String userId, ScheduledFuture<?> task) {
        userTasks.put(userId, task);
    }

    @Override
    public void stopUserTask(String userId) {
        ScheduledFuture<?> task = userTasks.remove(userId);
        if (task != null) {
            boolean cancelled = task.cancel(true);
            if (cancelled) {
                log.info("Loom-таск для пользователя {} успешно остановлен", userId);
            }
        } else {
            log.warn("Попытка остановить таск для {}, но активных задач не найдено", userId);
        }
    }

    @Override
    public void removeStateForUser(String userId) {
        userStates.remove(userId);
        ScheduledFuture<?> task = userTasks.remove(userId);
        if (task != null) {
            task.cancel(false);
        }
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
    public State createStateAfterMoveDown(State state, GameService gameService, String userId) {
        State freshState = getState(userId);

        if (freshState == null) {
            return null;
        }

        Optional<State> moveDownState = moveDown(freshState, getStepDown(freshState));

        if (moveDownState.isEmpty()) {
            Optional<State> newTetraminoState = newTetraminoState(freshState);
            if (newTetraminoState.isEmpty()) {
                freshState = buildState(freshState.getStage(), false, freshState.getGame());
                this.removeStateForUser(userId);
                setState(freshState, userId);
                return getState(userId);
            } else {
                freshState = newTetraminoState.orElse(freshState);
            }
        } else {
            freshState = moveDownState.orElse(freshState);
        }

        setState(freshState, userId);
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
    public boolean hasUserTask(String userId) {
        ScheduledFuture<?> task = userTasks.get(userId);
        return task != null && !task.isDone();
    }

    @Override
    public State dropDownState(String userId) {
        State currentState = getState(userId);
        if (currentState != null) {
            setState(dropDown(currentState).orElse(currentState), userId);
        }
        return getState(userId);
    }

    @Override
    public State moveRightState(String userId) {
        State currentState = getState(userId);
        if (currentState != null) {
            setState(moveRight(currentState).orElse(currentState), userId);
        }
        return getState(userId);
    }

    @Override
    public State moveLeftState(String userId) {
        State currentState = getState(userId);
        if (currentState != null) {
            setState(moveLeft(currentState).orElse(currentState), userId);
        }
        return getState(userId);
    }
    @Override
    public State rotateState(String userId) {
        State currentState = getState(userId);
        if (currentState != null) {
            setState(rotate(currentState).orElse(currentState), userId);
        }
        return getState(userId);
    }

    @Override
    public Optional<State> moveDownState(State state) {
        return moveDown(state, getStepDown(state));
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
        final char[][] c = java.util.Arrays.stream(state.getStage().getCells()).map(char[]::clone).toArray(char[][]::new);
        int startY = state.getStage().getTetraminoY();
        int startX = state.getStage().getTetraminoX();
        char[][] shape = state.getStage().getTetramino().getShape();

        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != '0') {
                    c[startY + y][startX + x] = shape[y][x];
                }
            }
        }
        return c;
    }

    private char[][] makeEmptyMatrix() {
        final char[][] c = new char[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            java.util.Arrays.fill(c[y], '0'); // Быстрое заполнение на уровне ОС
        }
        return c;
    }
    private Tetramino getTetramino0() {
        return TETRAMINO_POOL.get('0');
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
        final Tetramino t = getRandomTetramino();
        State newState = burryTetramino(state).orElse(state);

        // 🔥 ИСПРАВЛЕНИЕ БАГА: Выполняем схлопывание ровно один раз!
        Stage collapsedStage = collapseFilledLayers(newState);

        // Пересчитываем очки математически корректно, без побочных вызовов
        Game updatedGame = buildGame(newState.getGame().getPlayerName(), collapsedStage.getCollapsedLayersCount() * 10);
        newState = buildState(collapsedStage, newState.isRunning(), updatedGame);

        newState = initiateTetramino(t, (WIDTH - t.getShape().length) / 2, 0, newState).orElse(newState);
        return !checkCollision(newState, 0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    private Optional<State> restartWithNewTetramino(State state) {
        final Tetramino t = getRandomTetramino();
        State newState = burryTetramino(state).orElse(state);
        newState = initiateTetramino(t, (WIDTH - t.getShape().length) / 2, 0, newState).orElse(newState);
        return !checkCollision(newState, 0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    private Tetramino getRandomTetramino() {
        return TETRAMINO_POOL.get(TETRAMINO_KEYS[RANDOM.nextInt(TETRAMINO_KEYS.length)]);
    }

    public Optional<State> burryTetramino(State state) {
        return Optional.of(buildState(buildStage(drawTetraminoOnCells(state), state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount()), state.isRunning(), state.getGame()));
    }

    private Stage collapseFilledLayers(State state) {
        final char[][] originalCells = state.getStage().getCells();
        final char[][] newCells = new char[HEIGHT][WIDTH];

        int targetRow = HEIGHT - 1;
        int collapsedCount = 0;

        // Линейный проход снизу вверх вместо ресурсоемких стримов
        for (int srcRow = HEIGHT - 1; srcRow >= 0; srcRow--) {
            if (!isFull(originalCells[srcRow])) {
                System.arraycopy(originalCells[srcRow], 0, newCells[targetRow], 0, WIDTH);
                targetRow--;
            } else {
                collapsedCount++;
            }
        }

        // Заполняем оставшиеся пустые верхние строчки
        while (targetRow >= 0) {
            java.util.Arrays.fill(newCells[targetRow], '0');
            targetRow--;
        }

        return buildStage(newCells, state.getStage().getTetramino(), state.getStage().getTetraminoX(), state.getStage().getTetraminoY(), state.getStage().getCollapsedLayersCount() + collapsedCount);
    }

    private boolean isFull(char[] row) {
        for (char cell : row) {
            if (cell == '0') return false;
        }
        return true;
    }

    private boolean checkCollision(State state, int dx, int dy, boolean rotate) {
        final char[][] m = rotate ? rotateMatrix(state.getStage().getTetramino().getShape()) : state.getStage().getTetramino().getShape();
        int h = m.length;
        int w = m[0].length;
        int currentY = state.getStage().getTetraminoY();
        int currentX = state.getStage().getTetraminoX();
        char[][] cells = state.getStage().getCells();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (m[y][x] != '0') {
                    int nextY = currentY + y + dy;
                    int nextX = currentX + x + dx;

                    if (nextY >= HEIGHT || nextX < 0 || nextX >= WIDTH || cells[nextY][nextX] != '0') {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private char[][] rotateMatrix(char[][] m) {
        int h = m.length;
        int w = m[0].length;
        char[][] t = new char[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                t[w - x - 1][y] = m[y][x];
            }
        }
        return t;
    }

    private Game buildGame(String playerName, int playerScore) {
        return new Game(playerName, playerScore);
    }

    private SavedGame buildSavedGame(String playerName, int playerScore, char[][] cells) {
        return new SavedGame(playerName, playerScore, cells);
    }

    private int getStepDown(State state) {
        return state.getGame() == null ? 1 : state.getGame().getPlayerScore() / 10 + 1;
    }
}
