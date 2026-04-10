package com.app.game.tetris.dto;

import java.io.Serializable;

public record GameRecord(Long id, String playerName, int playerScore) implements Serializable {
}
