package com.github.fburato.justone.dtos.gamestates;

import com.github.fburato.justone.model.TurnPhase;

import java.util.List;
import java.util.Optional;

public record TurnDTO(
        String selectedWord,
        TurnPhase phase,
        List<PlayerWordDTO> providedHints,
        List<String> hintsToFilter,
        List<PlayerWordDTO> hintsToRemove,
        Optional<PlayerWordDTO> wordGuessed,
        List<TurnPlayerDTO> players
) {

}
