package com.app.game.tetris.dto;

import java.io.Serializable;

public record PlayerWelcomeDTO(String bestPlayer,
                               int bestScore,
                               String avatarBase64) implements Serializable {
}
