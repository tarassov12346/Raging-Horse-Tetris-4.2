package com.app.game.tetris.model;

import lombok.*;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {

    private Long id;

    @EqualsAndHashCode.Include
    @NonNull
    private String playerName;

    private final int playerScore;
}
