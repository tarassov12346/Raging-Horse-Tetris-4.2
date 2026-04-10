package com.app.game.tetris.dto;

import java.io.Serializable;

public record GameStateDTO(String playerName,
                           int playerScore,
                           boolean running,
                           char[][] cells) implements Serializable { }
