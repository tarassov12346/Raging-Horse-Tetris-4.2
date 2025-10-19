package com.app.game.tetris.model;


import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SavedGame {

    public SavedGame() {
    }

    private String playerName;

    private int playerScore;

    private char[][] cells;

    private SavedGame( String playerName,  int playerScore,  char[][] cells) {
        this.playerName = playerName;
        this.playerScore = playerScore;
        this.cells = cells;
    }

    public SavedGame buildSavedGame(String playerName, int playerScore, char[][] cells) {
        return new SavedGame(playerName, playerScore, cells);
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public char[][] getCells() {
        return cells;
    }

    @Override
    public String toString() {
        return "SavedGame{" +
                "playerName='" + playerName + '\'' +
                ", playerScore=" + playerScore +
                ", cells=" + Arrays.toString(cells) +
                '}';
    }
}
