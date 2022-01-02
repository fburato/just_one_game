package com.github.fburato.justone.model;

import java.util.List;

public record TurnPlayer(String playerId, List<TurnRole> roles) {
}
