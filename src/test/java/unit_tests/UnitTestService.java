package unit_tests;

import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.serviceImpl.Stage;
import com.app.game.tetris.serviceImpl.State;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.app.game.tetris.service.GameLogic.HEIGHT;
import static com.app.game.tetris.service.GameLogic.WIDTH;

public class UnitTestService {
    protected static final Logger log = Logger.getLogger(UnitTestService.class.getName());

    @Autowired
    private Stage stage;

    @Autowired
    private Tetramino tetramino;

    public Stage makeStageWithOnlyLeftUnfilledRows(int collapsedLayerCount) {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));
        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
                    switch (x % 3) {
                        case 0 -> c[y][x] = 'S';
                        case 1 -> c[y][x] = 'I';
                        default -> c[y][x] = '0';
                    }
                })
        );
        return stage.buildStage(c, getTetramino0(), 0, 0, collapsedLayerCount);
    }

    public Stage makeStageWith2FilledRows() {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 4).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));
        IntStream.range(HEIGHT - 4, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
                    switch (x % 3) {
                        case 0 -> c[y][x] = 'S';
                        case 1 -> c[y][x] = 'L';
                        case 2 -> c[y][x] = 'O';
                        default -> c[y][x] = 'I';
                    }
                })
        );
        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
                    switch (x % 3) {
                        case 0 -> c[y][x] = 'S';
                        case 1 -> c[y][x] = 'I';
                        default -> c[y][x] = '0';
                    }
                })
        );
        return stage.buildStage(c, getTetramino0(), 0, 0, 0);
    }

    public Stage makeStageWith3FilledRows() {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 5).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));
        IntStream.range(HEIGHT - 5, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
                    switch (x % 3) {
                        case 0 -> c[y][x] = 'O';
                        case 1 -> c[y][x] = 'S';
                        case 2 -> c[y][x] = 'I';
                        default -> c[y][x] = 'L';
                    }
                })
        );
        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
                    switch (x % 3) {
                        case 0 -> c[y][x] = 'S';
                        case 1 -> c[y][x] = 'I';
                        default -> c[y][x] = '0';
                    }
                })
        );
        return stage.buildStage(c, getTetramino0(), 0, 0, 0);
    }

    public State moveFarRight(State state, int moveCount) {
        state = state.moveRight().orElse(state);
        log.info("move tetramino 1 step right");
        log.info("tetramino position x=" + state.getStage().getTetraminoX() + " y=" + state.getStage().getTetraminoY());
        moveCount++;
        if (moveCount > 12) return state;
        return moveFarRight(state, moveCount);
    }

    public State moveFarLeft(State state, int moveCount) {
        state = state.moveLeft().orElse(state);
        log.info("move tetramino 1 step left");
        log.info("tetramino position x=" + state.getStage().getTetraminoX() + " y=" + state.getStage().getTetraminoY());
        moveCount++;
        if (moveCount > 12) return state;
        return moveFarLeft(state, moveCount);
    }

    public State moveDeepDown(State state, int moveCount) {
        state = state.moveDown(1).orElse(state);
        log.info("move tetramino 1 step down");
        log.info("tetramino position x=" + state.getStage().getTetraminoX() + " y=" + state.getStage().getTetraminoY());
        moveCount++;
        if (moveCount > 25) return state;
        return moveDeepDown(state, moveCount);
    }

    public Character getShapeTypeByTetramino(Tetramino tetramino) {
        final Map<Character, Tetramino> tetraminoMap = new HashMap<>();
        tetraminoMap.put('0', tetramino.buildTetramino(new char[][]{{'0'}}));
        tetraminoMap.put('I', tetramino.buildTetramino(new char[][]{{'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}, {'0', 'I', '0', '0'}}));
        tetraminoMap.put('J', tetramino.buildTetramino(new char[][]{{'0', 'J', '0'}, {'0', 'J', '0'}, {'J', 'J', '0'}}));
        tetraminoMap.put('L', tetramino.buildTetramino(new char[][]{{'0', 'L', '0'}, {'0', 'L', '0'}, {'0', 'L', 'L'}}));
        tetraminoMap.put('O', tetramino.buildTetramino(new char[][]{{'O', 'O'}, {'O', 'O'}}));
        tetraminoMap.put('S', tetramino.buildTetramino(new char[][]{{'0', 'S', 'S'}, {'S', 'S', '0'}, {'0', '0', '0'}}));
        tetraminoMap.put('T', tetramino.buildTetramino(new char[][]{{'0', '0', '0'}, {'T', 'T', 'T'}, {'0', 'T', '0'}}));
        tetraminoMap.put('Z', tetramino.buildTetramino(new char[][]{{'Z', 'Z', '0'}, {'0', 'Z', 'Z'}, {'0', '0', '0'}}));
        tetraminoMap.put('K', tetramino.buildTetramino(new char[][]{{'K', 'K', 'K'}, {'0', 'K', '0'}, {'0', 'K', '0'}}));
        for (Map.Entry<Character, Tetramino> entry : tetraminoMap.entrySet()) {
            if (Objects.equals(tetramino, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int countFilledCells(State state) {
        char[][] cells = state.getStage().getCells();
        int count = 0;
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                if (cells[i][j] == '0') {
                    count++;
                    break;
                }
            }
        }
        return HEIGHT - count;
    }

    public char[][] rotateMatrix(char[][] m) {
        final int h = m.length;
        final int w = m[0].length;
        final char[][] t = new char[h][w];
        IntStream.range(0, h).forEach(y -> IntStream.range(0, w).forEach(x -> t[w - x - 1][y] = m[y][x]));
        return t;
    }

    public String matrixToString(char[][] m) {
        StringBuilder expectedStr = new StringBuilder();
        expectedStr.append("{");
        for (char[] strings : m) {
            expectedStr.append("{");
            for (char s : strings) {
                expectedStr.append('"');
                expectedStr.append(s);
                expectedStr.append('"');
                expectedStr.append(',');
            }
            expectedStr.deleteCharAt(expectedStr.length() - 1);
            expectedStr.append("},");
        }
        expectedStr.deleteCharAt(expectedStr.length() - 1);
        expectedStr.append("}");
        return expectedStr.toString();
    }

    private Tetramino getTetramino0() {
        return tetramino.buildTetramino(new char[][]{{'0'}});
    }
}
