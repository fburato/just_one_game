package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.TurnPhase;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.engine.EngineTestUtils.kick;
import static com.github.fburato.justone.game.engine.EngineTestUtils.proceed;
import static org.assertj.core.api.Assertions.assertThat;

class KickTest {

    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());

    private final KickState testee = new KickState();
    private final Engine engine = new Engine(Try::success);
    private final Try<GameState> initState = engine.init(id, host, players, wordsToGuess);
    private final EngineTestUtils.RichState state = new EngineTestUtils.RichState(initState, testee).isValid();
    private final GameState stateBeforeAction = state.gameState().get();
    private final EngineTestUtils.RichState richStateWithTurn = new EngineTestUtils.RichState(
            new EngineTestUtils.RichState(initState, engine::execute)
                    .execute(proceed(host)).gameState(), testee);

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
