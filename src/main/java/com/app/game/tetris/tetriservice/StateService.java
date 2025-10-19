package com.app.game.tetris.tetriservice;

import com.app.game.tetris.model.Game;
import com.app.game.tetris.tetriserviceImpl.Stage;
import com.app.game.tetris.tetriserviceImpl.State;

import java.util.Optional;

public interface StateService extends GameLogic<Optional<State>>{
    State buildState(Stage stage, boolean isRunning, Game game);
    State start();
    State stop();
    Optional<State> createStateWithNewTetramino();
    Optional<State> restartWithNewTetramino();
    Optional<State> dropDown();
    Stage getStage();
    int getStepDown();
    void setStage(Stage stage);
}
