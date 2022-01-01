package com.github.fburato.justone;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGameState.class)
@JsonDeserialize(as = ImmutableGameState.class)
public interface GameState {

    String test();
}
