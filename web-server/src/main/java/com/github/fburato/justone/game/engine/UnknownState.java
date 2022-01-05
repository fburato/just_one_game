package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import io.vavr.control.Try;

class UnknownState implements EngineState {

    @Override
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        return Try.failure(new InvalidStateException(ErrorCode.UNRECOGNISED_STATE));
    }
}
