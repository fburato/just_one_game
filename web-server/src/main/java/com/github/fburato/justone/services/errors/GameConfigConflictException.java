package com.github.fburato.justone.services.errors;

public class GameConfigConflictException extends ConflictException {
    public GameConfigConflictException(String gameConfigId) {
        super("GameConfig", gameConfigId);
    }
}
