package com.app.game.tetris.dto;

import java.io.Serializable;

public record GameDataDTO(String bestPlayer, int bestScore) implements Serializable {

}
