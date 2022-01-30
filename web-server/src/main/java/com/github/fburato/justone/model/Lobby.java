package com.github.fburato.justone.model;

import java.util.List;

public record Lobby(String gameId, String host, List<PlayerConnection> playerConnections) {
}
