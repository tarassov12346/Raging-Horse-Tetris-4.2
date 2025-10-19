package com.app.game.tetris.tetriserviceImpl;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.tetriservice.GameLogic;
import com.app.game.tetris.tetriservice.StateService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class State implements StateService {
    private Stage stage;
    private boolean isRunning;
    private Game game;


    private State(Stage stage, boolean isRunning, Game game) {
        this.stage = Objects.requireNonNull(stage);
        this.isRunning = isRunning;
        this.game = game;
    }

    @Override
    public State buildState(Stage stage, boolean isRunning, Game game) {
        return new State(stage, isRunning, game);
    }

    private State() {
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return isRunning == state.isRunning && stage.equals(state.stage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage, isRunning);
    }

    @Override
    public Optional<State> moveLeft() {
        return !checkCollision(-1, 0, false) ? Optional.of(moveTetraminoLeft()) : Optional.empty();
    }

    @Override
    public Optional<State> moveRight() {
        return !checkCollision(1, 0, false) ? Optional.of(moveTetraminoRight()) : Optional.empty();
    }

    @Override
    public Optional<State> moveDown(int step) {
        int yToStepDown;
        for (yToStepDown = 0; (yToStepDown <= step) && (yToStepDown < GameLogic.HEIGHT); yToStepDown++) {
            if (checkCollision(0, yToStepDown, false)) break;
        }
        return !checkCollision(0, 1, false) ? Optional.of(moveTetraminoDown(yToStepDown - 1)) : Optional.empty();
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public Optional<State> rotate() {
        return !checkCollision(0, 0, true) ? Optional.of(rotateTetramino()) : Optional.empty();
    }

    @Override
    public Optional<State> initiateTetramino(Tetramino tetramino, int x, int y) {
        return Optional.of(buildState(stage.initiateTetramino(tetramino, x, y), isRunning, game));
    }

    @Override
    public Optional<State> burryTetramino() {
        return Optional.of(buildState(stage.burryTetramino(), isRunning, game));
    }

    @Override
    public Optional<State> collapseFilledLayers() {
        return Optional.of(buildState(stage.collapseFilledLayers(), isRunning, game));
    }

    @Override
    public boolean checkCollision(int dx, int dy, boolean rotate) {
        return stage.checkCollision(dx, dy, rotate);
    }

    public State start() {
        return buildState(stage, true, game);
    }

    public State stop() {
        return buildState(stage, false, game);
    }

    public Optional<State> createStateWithNewTetramino() {
        final Tetramino t = getRandomTetramino();
        final State newState = burryTetramino().orElse(this)
                .collapseFilledLayers().orElse(this)
                .updatePlayerScore()
                .initiateTetramino(t, (GameLogic.WIDTH - t.getShape().length) / 2, 0).orElse(this);
        return !newState.checkCollision(0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    public Optional<State> restartWithNewTetramino() {
        final Tetramino t = getRandomTetramino();
        final State newState = burryTetramino().orElse(this)
                .initiateTetramino(t, (GameLogic.WIDTH - t.getShape().length) / 2, 0).orElse(this);
        return !newState.checkCollision(0, 0, false) ? Optional.of(newState) : Optional.empty();
    }

    public Optional<State> dropDown() {
        int yToDropDown;
        for (yToDropDown = 0; yToDropDown < GameLogic.HEIGHT; yToDropDown++) {
            if (checkCollision(0, yToDropDown, false)) break;
        }
        return !checkCollision(0, yToDropDown - 1, false) ? Optional.of(moveTetraminoDown(yToDropDown - 1)) : Optional.empty();
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getStepDown() {
        return game == null ? 1 : getGame().getPlayerScore() / 10 + 1;
    }

    private State updatePlayerScore() {
        game.updatePlayerScore(stage.getCollapsedLayersCount());
        return new State(stage.collapseFilledLayers(), isRunning, game);
    }

    private State moveTetraminoDown(int yToMoveDown) {
        return buildState(stage.moveDown(yToMoveDown), isRunning, game);
    }

    private State moveTetraminoLeft() {
        return buildState(stage.moveLeft(), isRunning, game);
    }

    private State moveTetraminoRight() {
        return buildState(stage.moveRight(), isRunning, game);
    }

    private State rotateTetramino() {
        return buildState(stage.rotate(), isRunning, game);
    }

    private Tetramino getRandomTetramino() {
        final Map<Character, Tetramino> tetraminoMap = new HashMap<>();
        tetraminoMap.put('0', stage.getTetramino().buildTetramino(new char[][]{{'0'}}));
        tetraminoMap.put('I', stage.getTetramino().buildTetramino(new char[][]{{'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}}));
        tetraminoMap.put('J', stage.getTetramino().buildTetramino(new char[][]{{'0', 'J', '0'}, {'0', 'J', '0'}, {'J', 'J', '0'}}));
        tetraminoMap.put('L', stage.getTetramino().buildTetramino(new char[][]{{'0', 'L', '0'}, {'0', 'L', '0'}, {'0', 'L', 'L'}}));
        tetraminoMap.put('O', stage.getTetramino().buildTetramino(new char[][]{{'O', 'O'}, {'O', 'O'}}));
        tetraminoMap.put('S', stage.getTetramino().buildTetramino(new char[][]{{'0', 'S', 'S'}, {'S', 'S', '0'}, {'0', '0', '0'}}));
        tetraminoMap.put('T', stage.getTetramino().buildTetramino(new char[][]{{'0', '0', '0'}, {'T', 'T', 'T'}, {'0', 'T', '0'}}));
        tetraminoMap.put('Z', stage.getTetramino().buildTetramino(new char[][]{{'Z', 'Z', '0'}, {'0', 'Z', 'Z'}, {'0', '0', '0'}}));
        tetraminoMap.put('K', stage.getTetramino().buildTetramino(new char[][]{{'K', 'K', 'K'}, {'0', 'K', '0'}, {'0', 'K', '0'}}));
        final char[] tetraminos = "IJLOSTZK".toCharArray();
        return tetraminoMap.get(tetraminos[new Random().nextInt(tetraminos.length)]);
    }
}
