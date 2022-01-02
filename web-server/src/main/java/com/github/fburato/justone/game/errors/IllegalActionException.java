package com.github.fburato.justone.game.errors;

/**
 * Represents a failure due to the action well formed, but not
 * executable in the given state
 */
public class IllegalActionException extends EngineException {

    public IllegalActionException(ErrorCode... errorCodes) {
        super(errorCodes);
    }
}
