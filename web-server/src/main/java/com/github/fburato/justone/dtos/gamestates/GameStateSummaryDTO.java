package com.github.fburato.justone.dtos.gamestates;

import com.github.fburato.justone.model.GameStatus;

import java.util.List;

public record GameStateSummaryDTO(
        String id,

        GameStatus status,

        List<PlayerDTO> players,

        WordsDTO words
) {

}
