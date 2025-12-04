package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Stage {
    private char[][] cells;
    private Tetramino tetramino;
    private int tetraminoX;
    private int tetraminoY;
    private int collapsedLayersCount;
}
