package com.app.game.tetris.gameArtefactservice;

import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.model.State;

public interface GameArtefactService {
    void makeDesktopSnapshot(String fileNameDetail, PlayGameService playGameService, State state, String bestPlayerName, int bestPlayerScore);
}
