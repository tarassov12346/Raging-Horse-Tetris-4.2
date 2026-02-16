package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class Stage implements Serializable {
    private static final long serialVersionUID = 1L;
    private char[][] cells;
    private Tetramino tetramino;
    private int tetraminoX;
    private int tetraminoY;
    private int collapsedLayersCount;
}
