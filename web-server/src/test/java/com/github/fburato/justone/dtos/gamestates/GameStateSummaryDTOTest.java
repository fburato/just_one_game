package com.github.fburato.justone.dtos.gamestates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomGameStateSummaryDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GameStateSummaryDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should serialise into json")
    void serialise() {
        final GameStateSummaryDTO gameStateSummaryDTO = randomGameStateSummaryDTO();

        assertThatCode(() -> objectMapper.writeValueAsString(gameStateSummaryDTO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should deserialise json")
    void deserialise() throws Exception {
        final var deserialised =
                """
                        {
                          "id": "id",
                          "status": "IN_PROGRESS",
                          "players": [
                            {
                              "id": "p1",
                              "playerRole": "HOST"
                            },
                            {
                              "id": "p2",
                              "playerRole": "PLAYER"
                            }
                          ],
                          "words": {
                            "remaining": 1,
                            "correct": 2,
                            "wrong": 3
                          }
                        }
                        """;
        final var deserialisedObject = objectMapper.readValue(deserialised, GameStateSummaryDTO.class);
        assertThat(deserialisedObject).isEqualTo(ImmutableGameStateSummaryDTO.builder()
                .id("id")
                .status(GameStatus.IN_PROGRESS)
                .players(List.of(
                        new PlayerDTO("p1", PlayerRole.HOST),
                        new PlayerDTO("p2", PlayerRole.PLAYER)
                ))
                .words(new WordsDTO(1, 2, 3))
                .build());
    }

    @Test
    @DisplayName("should serialise and deserialise")
    void serialDeserialFlow() throws Exception {
        final var gameStateSummaryDto = randomGameStateSummaryDTO();
        final var serialised = objectMapper.writeValueAsString(gameStateSummaryDto);
        assertThat(objectMapper.readValue(serialised, GameStateSummaryDTO.class))
                .isEqualTo(gameStateSummaryDto);
    }
}