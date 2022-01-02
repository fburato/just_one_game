package com.github.fburato.justone.dtos.gamestates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableGameStateSummaryDTO.class)
@JsonDeserialize(as = ImmutableGameStateSummaryDTO.class)
public interface GameStateSummaryDTO {
    String id();

    GameStatus status();

    List<PlayerDTO> players();

    WordsDTO words();
}
