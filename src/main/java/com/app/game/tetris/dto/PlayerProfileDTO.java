package com.app.game.tetris.dto;

import java.io.Serializable;

public record PlayerProfileDTO(String playerName,
                               int playerScore)  implements Serializable {
}
