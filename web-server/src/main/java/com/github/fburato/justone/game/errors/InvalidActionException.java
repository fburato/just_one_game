package com.github.fburato.justone.game.errors;

/**
 * Represents a failure due to the action provided being not
 * processable because malformed in some way
 */
public class InvalidActionException extends EngineException {

    public InvalidActionException(ErrorCode... errorCodes) {
        super(errorCodes);
    }
}
