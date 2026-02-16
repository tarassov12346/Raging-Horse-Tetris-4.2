package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class State implements Serializable {
    private static final long serialVersionUID = 1L;
    private Stage stage;
    private boolean isRunning;
    private Game game;
}
