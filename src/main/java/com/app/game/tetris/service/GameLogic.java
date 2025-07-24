package com.app.game.tetris.service;

import com.app.game.tetris.model.Tetramino;

public interface GameLogic <T>{
    int WIDTH = 12;
    int HEIGHT = 20;
    T moveDown(int step);
    T rotate();
    T moveRight();
    T moveLeft();
    T collapseFilledLayers();
    T burryTetramino();
    T initiateTetramino(Tetramino tetramino, int x, int y);
    boolean checkCollision(int dx, int dy, boolean rotate);
}
