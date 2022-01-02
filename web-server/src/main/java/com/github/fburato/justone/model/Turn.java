package com.github.fburato.justone.model;

import java.util.List;
import java.util.Optional;

public record Turn(
        TurnPhase phase,
        List<PlayerWord> providedHints,
        List<String> hintsToFilter,
        List<PlayerWord> hintsToRemove,
        Optional<PlayerWord> wordGuessed,
        List<TurnPlayer> players
) {
}
