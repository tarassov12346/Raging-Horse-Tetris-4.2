package com.app.game.tetris.model;

import java.util.*;

public class State {
    private Stage stage;
    private boolean isRunning;
    private Game game;

    public State(Stage stage, boolean isRunning, Game game) {
        this.stage = Objects.requireNonNull(stage);
        this.isRunning = isRunning;
        this.game = game;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getStepDown() {
        return game == null ? 1 : getGame().getPlayerScore() / 10 + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return isRunning == state.isRunning && stage.equals(state.stage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage, isRunning);
    }
}
