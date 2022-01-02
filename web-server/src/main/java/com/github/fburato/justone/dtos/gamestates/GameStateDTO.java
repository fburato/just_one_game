package com.github.fburato.justone.dtos.gamestates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableGameStateDTO.class)
@JsonDeserialize(as = ImmutableGameStateDTO.class)
public interface GameStateDTO {
    String id();

    GameStatus status();

    List<PlayerDTO> players();

    List<TurnDTO> turns();

    List<String> wordsToGuess();

    int currentTurn();

    int totalTurns();
}
