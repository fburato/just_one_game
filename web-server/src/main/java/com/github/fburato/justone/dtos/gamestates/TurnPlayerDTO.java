package com.github.fburato.justone.dtos.gamestates;

import com.github.fburato.justone.model.TurnRole;

import java.util.List;

public record TurnPlayerDTO(String playerId, List<TurnRole> roles) {
}
