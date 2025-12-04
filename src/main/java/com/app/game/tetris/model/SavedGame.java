package com.app.game.tetris.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SavedGame {
    private String playerName;
    private int playerScore;
    private char[][] cells;
}
