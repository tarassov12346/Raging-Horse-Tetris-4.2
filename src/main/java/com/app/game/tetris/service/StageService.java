package com.app.game.tetris.service;

import com.app.game.tetris.model.Tetramino;
import com.app.game.tetris.serviceImpl.Stage;

public interface StageService extends GameLogic<Stage>{
    Stage buildStage(char[][] cells, Tetramino tetramino, int tetraminoX, int tetraminoY, int collapsedLayersCount);
    Stage buildStage(char[][] cells);
    char[][] drawTetraminoOnCells();
    char[][] getCells();
    Tetramino getTetramino();
    int getTetraminoX();
    int getTetraminoY();
    int getCollapsedLayersCount();
    void setCells(char[][] cells);

    Stage setTetramino(Tetramino tetramino, int x, int y);
}
