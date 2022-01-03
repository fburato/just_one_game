package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.EngineTestUtils.extractGuesser;
import static com.github.fburato.justone.game.EngineTestUtils.extractProviders;
import static com.github.fburato.justone.game.EngineTestUtils.extractRemover;
import static com.github.fburato.justone.game.EngineTestUtils.proceed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InitStateTest {

    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());
    private final List<String> allPlayers = Stream.concat(Stream.of(host), players.stream()).toList();

    private final InitState testee = new InitState();

    private final EngineTestUtils.RichState state = new EngineTestUtils.RichState(
            new Engine(mock(ActionCompiler.class)).init(id, host, players, wordsToGuess), testee).isValid();

    @Test
    @DisplayName("should allow to proceed with host")
    void proceedHost() {
        state.execute(proceed(host))
             .isValid();
    }

    @Test
    @DisplayName("should allow to proceed with root")
    void proceedRoot() {
        state.execute(proceed("root"))
             .isValid();
    }

    @Test
    @DisplayName("should not allow to proceed with non host")
    void proceedNonHost() {
        state.execute(proceed(players.get(0)))
             .isInvalid();
    }

    @Test
    @DisplayName("on proceed, should initialise turn 0 with SELECTION phase")
    void turn0WithSelection() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 assertThat(gameState.turns().size()).isOne();
                 final var turn0 = gameState.turns().get(0);
                 assertThat(turn0.phase()).isEqualTo(TurnPhase.SELECTION);
             });
    }

    @Test
    @DisplayName("on proceed, should have all words empty")
    void turn0WithAllEmpty() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 final var turn0 = gameState.turns().get(0);
                 assertThat(turn0.providedHints()).isEmpty();
                 assertThat(turn0.hintsToFilter()).isEmpty();
                 assertThat(turn0.hintsToRemove()).isEmpty();
                 assertThat(turn0.wordGuessed()).isEmpty();
             });
    }

    @Test
    @DisplayName("on proceed, should select only one of the existing players as guesser")
    void turn0WithOneGuesser() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 final var turn0 = gameState.turns().get(0);
                 final var guesser = extractGuesser(turn0);

                 assertThat(allPlayers).contains(guesser);
             });
    }

    @Test
    @DisplayName("on proceed, should select only one of the existing players as REMOVER")
    void turn0WithOneRemover() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 final var turn0 = gameState.turns().get(0);
                 final var remover = extractRemover(turn0);

                 assertThat(allPlayers).contains(remover);
             });
    }

    @Test
    @DisplayName("on proceed, should not select the same player as guesser and remover")
    void turn0DifferentGuesserRemover() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 final var turn0 = gameState.turns().get(0);
                 final var guesser = extractGuesser(turn0);
                 final var remover = extractRemover(turn0);

                 assertThat(guesser).isNotEqualTo(remover);
             });
    }

    @Test
    @DisplayName("on proceed, should mark every non guesser as a provider")
    void turn0NonIntersectProvidersAndGuesser() {
        state.execute(proceed(host))
             .isValidSatisfying(gameState -> {
                 final var turn0 = gameState.turns().get(0);
                 final var guesser = extractGuesser(turn0);
                 final var providers = extractProviders(turn0);

                 assertThat(providers.contains(guesser)).isFalse();
                 assertThat(allPlayers).containsExactlyInAnyOrderElementsOf(Stream.concat(
                         Stream.of(guesser),
                         providers.stream()
                 ).toList());
             });
    }

    @ParameterizedTest
    @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROCEED", "CANCEL_GAME"})
    @DisplayName("should reject actions not allowed")
    void rejectActions(TurnAction turnAction) {
        final var action = new Action<>(host, turnAction, String.class, randomString());
        state.execute(action)
             .isInvalidInstanceOfSatisfying(IllegalActionException.class, failure ->
                     assertThat(failure.errorCodes())
                             .containsExactly(ErrorCode.ILLEGAL_ACTION));
    }
}
