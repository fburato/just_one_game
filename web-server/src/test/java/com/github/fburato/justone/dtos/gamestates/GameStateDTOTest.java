package com.github.fburato.justone.dtos.gamestates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomGameStateDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GameStateDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should serialise into json")
    void serialiseJson() {
        assertThatCode(() -> objectMapper.writeValueAsString(randomGameStateDTO()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should deserialise from json")
    void deserialiseJson() throws Exception {
        final var json = """
                {
                  "id": "id",
                  "status": "CONCLUDED",
                  "players": [
                    {
                      "id": "p1",
                      "playerRole": "PLAYER"
                    }, {
                      "id": "p2",
                      "playerRole": "HOST"
                    }
                  ],
                  "turns": [
                    {
                      "selectedWord": "word",
                      "phase": "SELECTION",
                      "playerSelections": [
                        {
                          "playerId": "p1",
                          "word": "word1"
                        }, {
                          "playerId": "p2",
                          "word": "word2"
                        }
                      ],
                      "wordsToFilter": ["word1", "word2"],
                      "wordsToRemove": [
                        {
                          "playerId": "p3",
                          "word": "word3"
                        }, {
                          "playerId": "p4",
                          "word": "word4"
                        }
                      ],
                      "wordGuessed": {
                        "playerId": "p5",
                        "word": "word5"
                      },
                      "players": [
                        {
                          "playerId": "p6",
                          "roles": ["HOST", "GUESSER"]
                        }, {
                          "playerId": "p7",
                          "roles": ["REMOVER", "PROVIDER"]
                        }
                      ]
                    }, {
                      "selectedWord": "word1",
                      "phase": "GUESSING",
                      "playerSelections": [
                        {
                          "playerId": "p11",
                          "word": "word11"
                        }, {
                          "playerId": "p12",
                          "word": "word12"
                        }
                      ],
                      "wordsToFilter": ["word11", "word12"],
                      "wordsToRemove": [
                        {
                          "playerId": "p13",
                          "word": "word13"
                        }, {
                          "playerId": "p14",
                          "word": "word14"
                        }
                      ],
                      "wordGuessed": {
                        "playerId": "p15",
                        "word": "word15"
                      },
                      "players": [
                        {
                          "playerId": "p16",
                          "roles": ["HOST", "GUESSER"]
                        }, {
                          "playerId": "p17",
                          "roles": ["REMOVER", "PROVIDER"]
                        }
                      ]
                    }
                  ],
                  "wordsToGuess": ["word21", "word23"],
                  "currentTurn": 5,
                  "totalTurns": 34
                }
                """;
        final var deserialised = objectMapper.readValue(json, GameStateDTO.class);

        assertThat(deserialised).isEqualTo(ImmutableGameStateDTO.builder()
                .id("id")
                .status(GameStatus.CONCLUDED)
                .players(List.of(
                        new PlayerDTO("p1", PlayerRole.PLAYER),
                        new PlayerDTO("p2", PlayerRole.HOST)
                ))
                .turns(List.of(
                        ImmutableTurnDTO.builder()
                                .selectedWord("word")
                                .phase(TurnPhase.SELECTION)
                                .playerSelections(List.of(
                                        new PlayerWordDTO("p1", "word1"),
                                        new PlayerWordDTO("p2", "word2")
                                ))
                                .wordsToFilter(List.of("word1", "word2"))
                                .wordsToRemove(List.of(
                                        new PlayerWordDTO("p3", "word3"),
                                        new PlayerWordDTO("p4", "word4")
                                ))
                                .wordGuessed(new PlayerWordDTO("p5", "word5"))
                                .players(List.of(
                                        new TurnPlayerDTO("p6", List.of(TurnRole.HOST, TurnRole.GUESSER)),
                                        new TurnPlayerDTO("p7", List.of(TurnRole.REMOVER, TurnRole.PROVIDER))
                                ))
                                .build(),
                        ImmutableTurnDTO.builder()
                                .selectedWord("word1")
                                .phase(TurnPhase.GUESSING)
                                .playerSelections(List.of(
                                        new PlayerWordDTO("p11", "word11"),
                                        new PlayerWordDTO("p12", "word12")
                                ))
                                .wordsToFilter(List.of("word11", "word12"))
                                .wordsToRemove(List.of(
                                        new PlayerWordDTO("p13", "word13"),
                                        new PlayerWordDTO("p14", "word14")
                                ))
                                .wordGuessed(new PlayerWordDTO("p15", "word15"))
                                .players(List.of(
                                        new TurnPlayerDTO("p16", List.of(TurnRole.HOST, TurnRole.GUESSER)),
                                        new TurnPlayerDTO("p17", List.of(TurnRole.REMOVER, TurnRole.PROVIDER))
                                ))
                                .build()
                ))
                .wordsToGuess(List.of("word21", "word23"))
                .currentTurn(5)
                .totalTurns(34)
                .build());
    }

    @Test
    @DisplayName("should serialise and deserialise")
    void serialDeserialFlow() throws Exception {
        final var gameStateDto = randomGameStateDTO();
        final var serialised = objectMapper.writeValueAsString(gameStateDto);
        assertThat(objectMapper.readValue(serialised, GameStateDTO.class))
                .isEqualTo(gameStateDto);
    }
}