package com.github.fburato.justone.dtos.gamestates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableTurnDTO.class)
@JsonDeserialize(as = ImmutableTurnDTO.class)
public interface TurnDTO {
    String selectedWord();

    TurnPhase phase();

    List<PlayerWordDTO> playerSelections();

    List<String> wordsToFilter();

    List<PlayerWordDTO> wordsToRemove();

    PlayerWordDTO wordGuessed();

    List<TurnPlayerDTO> players();
}
