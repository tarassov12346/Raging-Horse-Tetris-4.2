package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class Tetramino implements Serializable {
    private static final long serialVersionUID = 1L;
    char[][] shape;
}
