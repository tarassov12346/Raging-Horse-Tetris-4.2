package com.app.game.tetris.gameArtefactservice;

import com.app.game.tetris.serviceImpl.State;

public interface GameArtefactService {
    void makeDesktopSnapshot(String fileNameDetail, State state, String bestPlayerName, int bestPlayerScore);
}
