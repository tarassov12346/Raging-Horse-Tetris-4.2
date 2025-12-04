package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class State {
    private Stage stage;
    private boolean isRunning;
    private Game game;
}
