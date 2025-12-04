package com.app.game.tetris.model;

import java.util.Arrays;
import java.util.Objects;

public class Stage {
    private char[][] cells;
    private Tetramino tetramino;
    private int tetraminoX;
    private int tetraminoY;
    private int collapsedLayersCount;

    public Stage(char[][] cells, Tetramino tetramino, int tetraminoX, int tetraminoY, int collapsedLayersCount) {
        this.cells = cells;
        this.tetramino = tetramino;
        this.tetraminoX = tetraminoX;
        this.tetraminoY = tetraminoY;
        this.collapsedLayersCount = collapsedLayersCount;
    }

    public char[][] getCells() {
        return cells;
    }

    public void setCells(char[][] cells) {
        this.cells = cells;
    }

    public Tetramino getTetramino() {
        return tetramino;
    }

    public int getTetraminoX() {
        return tetraminoX;
    }

    public int getTetraminoY() {
        return tetraminoY;
    }

    public int getCollapsedLayersCount() {
        return collapsedLayersCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stage stage = (Stage) o;
        return tetraminoX == stage.tetraminoX && tetraminoY == stage.tetraminoY && collapsedLayersCount == stage.collapsedLayersCount && Arrays.deepEquals(cells, stage.cells) && tetramino.equals(stage.tetramino);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tetramino, tetraminoX, tetraminoY, collapsedLayersCount);
        result = 31 * result + Arrays.hashCode(cells);
        return result;
    }
}
