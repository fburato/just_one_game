package com.github.fburato.justone;

import com.github.fburato.justone.dtos.gamestates.GameStateDTO;
import com.github.fburato.justone.dtos.gamestates.GameStateSummaryDTO;
import com.github.fburato.justone.dtos.gamestates.GameStatus;
import com.github.fburato.justone.dtos.gamestates.ImmutableGameStateDTO;
import com.github.fburato.justone.dtos.gamestates.ImmutableGameStateSummaryDTO;
import com.github.fburato.justone.dtos.gamestates.ImmutableTurnDTO;
import com.github.fburato.justone.dtos.gamestates.PlayerDTO;
import com.github.fburato.justone.dtos.gamestates.PlayerRole;
import com.github.fburato.justone.dtos.gamestates.PlayerWordDTO;
import com.github.fburato.justone.dtos.gamestates.TurnDTO;
import com.github.fburato.justone.dtos.gamestates.TurnPhase;
import com.github.fburato.justone.dtos.gamestates.TurnPlayerDTO;
import com.github.fburato.justone.dtos.gamestates.TurnRole;
import com.github.fburato.justone.dtos.gamestates.WordsDTO;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

import static org.apache.commons.lang3.RandomUtils.nextInt;

public final class RandomUtils {

    public static String randomString() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    public static <T extends Enum<T>> T randomEnum(Class<T> tClass) {
        final var values = tClass.getEnumConstants();
        return values[nextInt(0, values.length)];
    }

    public static PlayerDTO randomPlayerDto() {
        return new PlayerDTO(randomString(), randomEnum(PlayerRole.class));
    }

    public static GameStateSummaryDTO randomGameStateSummaryDTO() {
        return ImmutableGameStateSummaryDTO.builder()
                .id(randomString())
                .status(randomEnum(GameStatus.class))
                .players(List.of(
                        randomPlayerDto(),
                        randomPlayerDto()
                ))
                .words(new WordsDTO(nextInt(0, 40), nextInt(0, 40), nextInt(0, 40)))
                .build();
    }

    public static PlayerWordDTO randomPlayerWordDTO() {
        return new PlayerWordDTO(randomString(), randomString());
    }

    public static TurnPlayerDTO randomTurnPlayerDTO() {
        return new TurnPlayerDTO(randomString(), List.of(randomEnum(TurnRole.class), randomEnum(TurnRole.class)));
    }

    public static TurnDTO randomTurnDTO() {
        return ImmutableTurnDTO.builder()
                .selectedWord(randomString())
                .phase(randomEnum(TurnPhase.class))
                .playerSelections(List.of(
                        randomPlayerWordDTO(),
                        randomPlayerWordDTO()
                ))
                .wordsToFilter(List.of(randomString(), randomString()))
                .wordsToRemove(List.of(
                        randomPlayerWordDTO(),
                        randomPlayerWordDTO()
                ))
                .wordGuessed(randomPlayerWordDTO())
                .players(List.of(
                        randomTurnPlayerDTO(),
                        randomTurnPlayerDTO()
                )).build();
    }

    public static GameStateDTO randomGameStateDTO() {
        return ImmutableGameStateDTO.builder()
                .id(randomString())
                .status(randomEnum(GameStatus.class))
                .players(List.of(
                        randomPlayerDto(),
                        randomPlayerDto()
                ))
                .turns(List.of(
                        randomTurnDTO(),
                        randomTurnDTO()
                ))
                .wordsToGuess(List.of(randomString(), randomString()))
                .currentTurn(nextInt(0, 30))
                .totalTurns(nextInt(0, 40))
                .build();
    }
}
