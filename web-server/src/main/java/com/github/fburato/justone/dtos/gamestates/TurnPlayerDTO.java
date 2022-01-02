package com.github.fburato.justone.dtos.gamestates;

import java.util.List;

public record TurnPlayerDTO(String playerId, List<TurnRole> roles) {
}
