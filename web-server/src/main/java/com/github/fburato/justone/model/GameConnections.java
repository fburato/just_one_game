package com.github.fburato.justone.model;

import java.util.List;

public record GameConnections(String gameId, List<PlayerStatus> players) {

    public enum PlayerStatus {
        CONNECTED, ACCEPTED
    }

    public record Player(String playerId, PlayerStatus playerStatus) {

    }
}
