package com.app.game.tetris.model;

import lombok.Value;
import org.springframework.stereotype.Component;

@Component
@Value
public class Tetramino {
    char[][] shape;

    public char[][] getShape() {
        return shape;
    }

    private Tetramino(char[][] shape) {
        this.shape = shape;
    }

    public Tetramino buildTetramino(char[][] shape){
        return new Tetramino(shape);
    }
}
