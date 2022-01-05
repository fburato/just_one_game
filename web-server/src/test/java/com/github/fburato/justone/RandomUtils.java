package com.github.fburato.justone;

import com.github.fburato.justone.dtos.gamestates.GameStateDTO;
import com.github.fburato.justone.dtos.gamestates.GameStateSummaryDTO;
import com.github.fburato.justone.dtos.gamestates.PlayerDTO;
import com.github.fburato.justone.dtos.gamestates.PlayerWordDTO;
import com.github.fburato.justone.dtos.gamestates.TurnDTO;
import com.github.fburato.justone.dtos.gamestates.TurnPlayerDTO;
import com.github.fburato.justone.dtos.gamestates.WordsDTO;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Optional;

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
        return new GameStateSummaryDTO(
                randomString(),
                randomEnum(GameStatus.class),
                List.of(
                        randomPlayerDto(),
                        randomPlayerDto()
                ),
                new WordsDTO(nextInt(0, 40), nextInt(0, 40), nextInt(0, 40)));
    }

    public static PlayerWordDTO randomPlayerWordDTO() {
        return new PlayerWordDTO(randomString(), randomString());
    }

    public static TurnPlayerDTO randomTurnPlayerDTO() {
        return new TurnPlayerDTO(randomString(), List.of(randomEnum(TurnRole.class), randomEnum(TurnRole.class)));
    }

    public static TurnDTO randomTurnDTO() {
        return new TurnDTO(
                randomString(),
                randomEnum(TurnPhase.class),
                List.of(
                        randomPlayerWordDTO(),
                        randomPlayerWordDTO()
                ),
                List.of(randomString(), randomString()),
                List.of(
                        randomPlayerWordDTO(),
                        randomPlayerWordDTO()
                ),
                Optional.of(randomPlayerWordDTO()),
                List.of(
                        randomTurnPlayerDTO(),
                        randomTurnPlayerDTO()
                ));
    }

    public static GameStateDTO randomGameStateDTO() {
        return new GameStateDTO(
                randomString(),
                randomEnum(GameStatus.class),
                List.of(
                        randomPlayerDto(),
                        randomPlayerDto()
                ),
                List.of(
                        randomTurnDTO(),
                        randomTurnDTO()
                ),
                List.of(randomString(), randomString()),
                nextInt(0, 30),
                nextInt(0, 40));
    }

    public static Player randomPlayer() {
        return new Player(randomString(), randomEnum(PlayerRole.class));
    }

    public static PlayerWord randomPlayerWord() {
        return new PlayerWord(randomString(), randomString());
    }

    public static TurnPlayer randomTurnPlayer() {
        return new TurnPlayer(randomString(), List.of(randomEnum(TurnRole.class)));
    }

    public static Turn randomTurn() {
        return new Turn(
                randomEnum(TurnPhase.class),
                List.of(randomPlayerWord(), randomPlayerWord()),
                List.of(randomString(), randomString()),
                List.of(randomPlayerWord(), randomPlayerWord()),
                Optional.of(randomPlayerWord()),
                List.of(randomTurnPlayer(), randomTurnPlayer())
        );
    }

    public static GameState randomGameState() {
        return new GameState(
                randomString(),
                randomEnum(GameStatus.class),
                List.of(randomPlayer(), randomPlayer()),
                List.of(randomTurn(), randomTurn()),
                List.of(randomString(), randomString()),
                nextInt(0, 60)
        );
    }
}
