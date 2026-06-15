package com.app.game.tetris.gameArtefactservice;

import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.model.State;

import java.util.concurrent.CompletableFuture;

public interface GameArtefactService {
    CompletableFuture<Void> makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore);
}
