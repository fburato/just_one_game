package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import io.vavr.control.Try;

interface EngineState {
    Try<GameState> execute(GameState gameState, Action<?> action);
}
