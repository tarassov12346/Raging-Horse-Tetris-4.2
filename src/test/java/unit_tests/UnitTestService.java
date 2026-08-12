package unit_tests;


import com.app.game.tetris.model.Stage;
import com.app.game.tetris.model.State;
import com.app.game.tetris.model.Tetramino;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.IntStream;

@Slf4j
@Service
public class UnitTestService {

    // Жестко фиксируем размеры для генерации тестовых стаканов
    private static final int HEIGHT = 20;
    private static final int WIDTH = 10;

    /**
     * Создает стакан, где нижние 2 ряда заполнены частично,
     * а 2 ряда над ними (HEIGHT-4 до HEIGHT-2) заполнены полностью ('S','L','O').
     */
    public Stage makeStageWith2FilledRows() {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 4).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));

        IntStream.range(HEIGHT - 4, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
            switch (x % 3) {
                case 0 -> c[y][x] = 'S';
                case 1 -> c[y][x] = 'L';
                default -> c[y][x] = 'O'; // default покрывает случай x % 3 == 2, ряд полный
            }
        }));

        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
            switch (x % 3) {
                case 0 -> c[y][x] = 'S';
                case 1 -> c[y][x] = 'I';
                default -> c[y][x] = '0';
            }
        }));
        return new Stage(c, getTetramino0(), 0, 0, 0);
    }

    /**
     * Создает стакан с 3 полностью заполненными рядами для проверки тройного схлопывания.
     */
    public Stage makeStageWith3FilledRows() {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 5).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));

        IntStream.range(HEIGHT - 5, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
            switch (x % 3) {
                case 0 -> c[y][x] = 'O';
                case 1 -> c[y][x] = 'S';
                default -> c[y][x] = 'I'; // default покрывает случай x % 3 == 2, ряд полный
            }
        }));

        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
            switch (x % 3) {
                case 0 -> c[y][x] = 'S';
                case 1 -> c[y][x] = 'I';
                default -> c[y][x] = '0';
            }
        }));
        return new Stage(c, getTetramino0(), 0, 0, 0);
    }

    /**
     * Возвращает стакан после схлопывания слоев (остаются только нижние незаполненные ряды).
     */
    public Stage makeStageWithOnlyLeftUnfilledRows(int collapsedLayerCount) {
        final char[][] c = new char[HEIGHT][WIDTH];
        IntStream.range(0, HEIGHT - 2).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> c[y][x] = '0'));
        IntStream.range(HEIGHT - 2, HEIGHT).forEach(y -> IntStream.range(0, WIDTH).forEach(x -> {
            switch (x % 3) {
                case 0 -> c[y][x] = 'S';
                case 1 -> c[y][x] = 'I';
                default -> c[y][x] = '0';
            }
        }));
        return new Stage(c, getTetramino0(), 0, 0, collapsedLayerCount);
    }

    /**
     * Считает количество полностью заполненных рядов в стакане.
     */
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

    /**
     * Поворачивает матрицу фигуры по часовой стрелке.
     */
    public char[][] rotateMatrix(char[][] m) {
        final int h = m.length;
        final int w = m.length;
        final char[][] t = new char[h][w];
        IntStream.range(0, h).forEach(y -> IntStream.range(0, w).forEach(x -> t[w - x - 1][y] = m[y][x]));
        return t;
    }

    /**
     * Превращает матрицу в читаемую строку для логирования.
     */
    public String matrixToString(char[][] m) {
        StringBuilder expectedStr = new StringBuilder();
        expectedStr.append("{");
        for (char[] strings : m) {
            expectedStr.append("{");
            for (char s : strings) {
                expectedStr.append('"').append(s).append('"').append(',');
            }
            expectedStr.deleteCharAt(expectedStr.length() - 1);
            expectedStr.append("},");
        }
        expectedStr.deleteCharAt(expectedStr.length() - 1);
        expectedStr.append("}");
        return expectedStr.toString();
    }

    public Tetramino getTetramino0() {
        return new Tetramino(new char[][]{{'0'}});
    }
}
