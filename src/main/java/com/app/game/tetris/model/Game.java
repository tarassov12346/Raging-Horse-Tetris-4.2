package com.app.game.tetris.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {
    private Long id;

    @NonNull
    @EqualsAndHashCode.Include
    private String playerName;

    @NonNull
    private int playerScore;
}
