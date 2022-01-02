package com.github.fburato.justone.game.errors;

/**
 * Represents a failure due to the state provided being not
 * processable because malformed in some way
 */
public class InvalidStateException extends EngineException {

    public InvalidStateException(ErrorCode... errorCodes) {
        super(errorCodes);
    }
}
