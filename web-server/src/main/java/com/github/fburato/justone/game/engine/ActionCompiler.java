package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.InvalidActionException;
import com.github.fburato.justone.model.Action;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;

public interface ActionCompiler {

    ActionCompiler DEFAULT_ACTION_COMPILER = new ActionCompiler() {
        @Override
        public <T> Try<Action<T>> compile(Action<T> action) {
            final List<ErrorCode> errors = new ArrayList<>();
            if (!(action.payload() == null && action.payloadType() == Void.class)
                    && !action.payloadType().isInstance(action.payload())) {
                errors.add(ErrorCode.PAYLOAD_TYPE_MISMATCH);
            }
            switch (action.playerAction()) {
                case PROCEED, CANCEL_GAME, CANCEL_PROVIDED_HINT -> {
                    if (action.payloadType() != Void.class) {
                        errors.add(ErrorCode.INVALID_PAYLOAD);
                    }
                }
                default -> {
                    if (action.payloadType() != String.class) {
                        errors.add(ErrorCode.INVALID_PAYLOAD);
                    }
                }
            }
            if (errors.size() == 0) {
                return Try.success(action);
            }
            return Try.failure(new InvalidActionException(errors.toArray(ErrorCode[]::new)));
        }
    };

    <T> Try<Action<T>> compile(Action<T> action);
}
