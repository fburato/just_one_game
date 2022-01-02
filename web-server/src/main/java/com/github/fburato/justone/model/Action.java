package com.github.fburato.justone.model;

public record Action<T>(
        String playerId,
        TurnAction playerAction,
        Class<T> payloadType,
        T payload
) {
}
