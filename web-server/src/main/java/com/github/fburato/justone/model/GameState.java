package com.github.fburato.justone.model;

import java.util.List;

public record GameState(
        String id,
        GameStatus status,
        List<Player> players,
        List<Turn> turns,
        List<String> wordsToGuess,
        int currentTurn) {
}
