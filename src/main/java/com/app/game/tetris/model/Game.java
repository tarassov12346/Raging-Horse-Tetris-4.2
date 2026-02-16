package com.app.game.tetris.model;

import lombok.*;
import java.io.Serializable;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    @EqualsAndHashCode.Include
    @NonNull
    private String playerName;

    private final int playerScore;
}
