package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.EngineTestUtils.kick;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KickTest {

    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());

    private final KickState testee = new KickState();
    private final EngineTestUtils.RichState state = new EngineTestUtils.RichState(
            new Engine(mock(ActionCompiler.class)).init(id, host, players, wordsToGuess), testee).isValid();
    private final GameState stateWithTurn = new GameState(id,
                                                          GameStatus.IN_PROGRESS,
                                                          List.of(
                                                                  new Player(host, PlayerRole.HOST),
                                                                  new Player(players.get(0), PlayerRole.PLAYER),
                                                                  new Player(players.get(1), PlayerRole.PLAYER)
                                                          ),
                                                          List.of(
                                                                  new Turn(
                                                                          TurnPhase.SELECTION,
                                                                          List.of(),
                                                                          List.of(),
                                                                          List.of(),
                                                                          Optional.empty(),
                                                                          List.of(
                                                                                  new TurnPlayer(host,
                                                                                                 List.of(TurnRole.GUESSER)),
                                                                                  new TurnPlayer(players.get(0),
                                                                                                 List.of(TurnRole.PROVIDER,
                                                                                                         TurnRole.REMOVER)),
                                                                                  new TurnPlayer(players.get(1),
                                                                                                 List.of(TurnRole.PROVIDER))
                                                                          )
                                                                  )
                                                          ),
                                                          List.of(randomString(), randomString()),
                                                          0
    );
    private final EngineTestUtils.RichState richStateWithTurn = new EngineTestUtils.RichState(
            Try.success(stateWithTurn), testee);

    private final GameState stateBeforeAction = state.gameState().get();

    @Test
    @DisplayName("do nothing if player does not exist")
    void nothingOnNotExisting() {
        state.execute(kick(host, randomString()))
             .isValidSatisfying(gameState ->
                                        assertThat(gameState).isEqualTo(stateBeforeAction));
    }

    @Test
    @DisplayName("fail if kick would reduce players to 1")
    void failOnLessThan2PlayerRemaining() {
        state.execute(kick(host, players.get(0)))
             .isValid()
             .execute(kick(host, players.get(1)))
             .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                     assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));
    }

    @Test
    @DisplayName("remove player from game state")
    void removeFromGameState() {
        state.execute(kick(host, players.get(0)))
             .isValidSatisfying(gameState ->
                                        assertThat(gameState.players().contains(
                                                new Player(players.get(0), PlayerRole.PLAYER)))
                                                .isFalse());
    }

    @Test
    @DisplayName("remove player from current turn if present")
    void removePlayerFromCurrentTurn() {
        richStateWithTurn.isValid()
                         .execute(kick(host, players.get(0)))
                         .isValidSatisfying(gameState -> {
                             final var turn0 = gameState.turns().get(0);
                             assertThat(
                                     turn0.players().stream()
                                          .anyMatch(p -> StringUtils.equals(p.playerId(), players.get(0))))
                                     .isFalse();
                         });
    }

    @Test
    @DisplayName("conclude current turn if present")
    void concludeCurrentTurn() {
        richStateWithTurn
                .execute(kick(host, players.get(0)))
                .isValidSatisfying(gameState -> {
                    final var turn0 = gameState.turns().get(0);
                    assertThat(turn0.phase()).isEqualTo(TurnPhase.CONCLUSION);
                    assertThat(turn0.wordGuessed()).contains(new PlayerWord("root", ""));
                });
    }

    @Test
    @DisplayName("promote other player to host if host is removed")
    void promoteNewHostIfRemoved() {
        state.execute(kick(host, host))
             .isValidSatisfying(gameState -> {
                 assertThat(gameState.players().contains(new Player(host, PlayerRole.HOST)))
                         .isFalse();
                 final var newHost = gameState.players().stream()
                                              .filter(p -> p.playerRole() == PlayerRole.HOST)
                                              .findFirst().orElseThrow();
                 assertThat(players).contains(newHost.id());
             });
    }
}
