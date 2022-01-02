package com.github.fburato.justone.dtos.gamestates;

import com.github.fburato.justone.model.GameStatus;

import java.util.List;

public record GameStateDTO(
        String id,
        GameStatus status,
        List<PlayerDTO> players,
        List<TurnDTO> turns,
        List<String> wordsToGuess,
        int currentTurn,
        int totalTurns) {
}
