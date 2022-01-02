package com.github.fburato.justone.model;

import java.util.List;

public record GameStateSummary(
        String id,
        GameStatus status,
        List<Player> players,
        Words words
) {
}
