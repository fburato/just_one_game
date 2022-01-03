package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.EngineTestUtils.hint;
import static com.github.fburato.justone.game.EngineTestUtils.proceed;
import static com.github.fburato.justone.game.EngineTestUtils.removeHint;
import static com.github.fburato.justone.model.Builders.gameStateBuilder;
import static com.github.fburato.justone.model.Builders.turnBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class SelectionTest {
    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());
    private final Engine engine = new Engine(Try::success);
    private final EngineTestUtils.RichState initialState = new EngineTestUtils.RichState(
            engine.init(id, host, players, wordsToGuess), engine::execute)
            .execute(proceed(host));
    private final GameState stateWithSelection = gameStateBuilder(initialState.gameState().get())
            .with(gsb -> {
                final var turns = initialState.gameState().get().turns();
                gsb.currentTurn = 1;
                gsb.turns = new ArrayList<>(turns);
                gsb.turns.add(turnBuilder(turns.get(0)).build());
            }).build();
    private final SelectionState testee = new SelectionState();

    private EngineTestUtils.RichState richStateOf(GameState gameState) {
        return new EngineTestUtils.RichState(Try.success(gameState), testee);
    }

    @Test
    @DisplayName("should fail with ILLEGAL_ACTION if current turn is not in selection phase")
    void failOnNotSelection() {
        final var gameStateInWrongPhase = gameStateBuilder(stateWithSelection)
                .with(gsb -> {
                    gsb.turns = new ArrayList<>(stateWithSelection.turns());
                    gsb.turns.set(1, turnBuilder(stateWithSelection.turns().get(0)).with(tb -> {
                        tb.phase = TurnPhase.CONCLUSION;
                    }).build());
                }).build();
        richStateOf(gameStateInWrongPhase)
                .execute(hint(host, randomString()))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));

    }

    @ParameterizedTest
    @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROVIDE_HINT", "CANCEL_PROVIDED_HINT"})
    @DisplayName("should fail with ILLEGAL_ACTION if any other action is provided")
    void failOnIllegalAction(TurnAction turnAction) {
        richStateOf(stateWithSelection)
                .execute(new Action<>(host, turnAction, Object.class, null))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));
    }

    @Test
    @DisplayName("should fail with UNAUTHORISED_ACTION if player is not provider when providing hint")
    void failOnNotProvider() {
        final var notProvider = stateWithSelection.turns().get(1).players().stream()
                                                  .filter(tp -> !tp.roles().contains(TurnRole.PROVIDER))
                                                  .findFirst().orElseThrow();
        richStateOf(stateWithSelection)
                .execute(hint(notProvider.playerId(), randomString()))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNAUTHORISED_ACTION));
    }

    @Test
    @DisplayName("should fail with UNAUTHORISED_ACTION if player is not provider when removing hint")
    void failOnNotProviderRemoval() {
        final var notProvider = stateWithSelection.turns().get(1).players().stream()
                                                  .filter(tp -> !tp.roles().contains(TurnRole.PROVIDER))
                                                  .findFirst().orElseThrow();
        richStateOf(stateWithSelection)
                .execute(removeHint(notProvider.playerId()))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNAUTHORISED_ACTION));
    }

    @Test
    @DisplayName("should add hint to state if player is provider to current turn")
    void addHintByProvider() {
        final var provider = stateWithSelection.turns().get(1).players().stream()
                                               .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                               .findFirst().orElseThrow();
        final var hint = randomString();
        richStateOf(stateWithSelection)
                .execute(hint(provider.playerId(), hint))
                .isValidSatisfying(gameState -> {
                    final var currentTurn = gameState.turns().get(gameState.currentTurn());
                    assertThat(currentTurn.providedHints())
                            .containsExactly(new PlayerWord(provider.playerId(), hint));
                });
    }

    @Test
    @DisplayName("should replace provided hint to state if player already has provided one")
    void replaceHint() {
        final var provider = stateWithSelection.turns().get(1).players().stream()
                                               .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                               .findFirst().orElseThrow();
        final var hint1 = randomString();
        final var hint2 = randomString();
        richStateOf(stateWithSelection)
                .execute(hint(provider.playerId(), hint1))
                .execute(hint(provider.playerId(), hint2))
                .isValidSatisfying(gameState -> {
                    final var currentTurn = gameState.turns().get(gameState.currentTurn());
                    assertThat(currentTurn.providedHints())
                            .containsExactly(new PlayerWord(provider.playerId(), hint2));
                });
    }

    @Test
    @DisplayName("should add multiple provided hints by multiple providers")
    void multipleProviders() {
        final var provider1 = stateWithSelection.turns().get(1).players().stream()
                                                .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                                .findFirst().orElseThrow();
        final var provider2 = stateWithSelection.turns().get(1).players().stream()
                                                .filter(tp -> tp.roles().contains(TurnRole.PROVIDER) && !tp.playerId()
                                                                                                           .equals(provider1.playerId()))
                                                .findFirst().orElseThrow();
        final var hint1 = randomString();
        final var hint2 = randomString();
        richStateOf(stateWithSelection)
                .execute(hint(provider1.playerId(), hint1))
                .execute(hint(provider2.playerId(), hint2))
                .isValidSatisfying(gameState -> {
                    final var currentTurn = gameState.turns().get(gameState.currentTurn());
                    assertThat(currentTurn.providedHints())
                            .containsExactly(
                                    new PlayerWord(provider1.playerId(), hint1),
                                    new PlayerWord(provider2.playerId(), hint2)
                            );
                });
    }

    @Test
    @DisplayName("should do nothing on remove hint if player has not provided any hint")
    void nothingOnRemoveUnexisting() {
        final var provider = stateWithSelection.turns().get(1).players().stream()
                                               .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                               .findFirst().orElseThrow();
        richStateOf(stateWithSelection)
                .execute(removeHint(provider.playerId()))
                .isValid();
    }

    @Test
    @DisplayName("should remove hint if it was provided before")
    void removeHintTest() {
        final var provider1 = stateWithSelection.turns().get(1).players().stream()
                                                .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                                .findFirst().orElseThrow();
        final var provider2 = stateWithSelection.turns().get(1).players().stream()
                                                .filter(tp -> tp.roles().contains(TurnRole.PROVIDER) && !tp.playerId()
                                                                                                           .equals(provider1.playerId()))
                                                .findFirst().orElseThrow();
        final var hint1 = randomString();
        final var hint2 = randomString();
        richStateOf(stateWithSelection)
                .execute(hint(provider1.playerId(), hint1))
                .isValidSatisfying(gameState -> {
                    final var currentTurn = gameState.turns().get(gameState.currentTurn());
                    assertThat(currentTurn.providedHints())
                            .containsExactly(
                                    new PlayerWord(provider1.playerId(), hint1)
                            );
                })
                .execute(removeHint(provider1.playerId()))
                .isValidSatisfying(gameState -> {
                    final var currentTurn = gameState.turns().get(gameState.currentTurn());
                    assertThat(currentTurn.providedHints())
                            .isEmpty();
                });
    }

    @Nested
    @DisplayName("when all hints have been provided")
    class AllHintsProvidedTest {

        @Test
        @DisplayName("should transition turn to REMOVAL phase")
        void transitionToRemoval() {
            final var provider1 = stateWithSelection.turns().get(1).players().stream()
                                                    .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                                    .findFirst().orElseThrow();
            final var provider2 = stateWithSelection.turns().get(1).players().stream()
                                                    .filter(tp -> tp.roles()
                                                                    .contains(TurnRole.PROVIDER) && !tp.playerId()
                                                                                                       .equals(provider1.playerId()))
                                                    .findFirst().orElseThrow();
            richStateOf(stateWithSelection)
                    .execute(hint(provider1.playerId(), randomString()))
                    .isValidSatisfying(gameState -> {
                        final var currentTurn = gameState.turns().get(gameState.currentTurn());
                        assertThat(currentTurn.phase()).isEqualTo(TurnPhase.SELECTION);
                    })
                    .execute(hint(provider2.playerId(), randomString()))
                    .isValidSatisfying(gameState -> {
                        final var currentTurn = gameState.turns().get(gameState.currentTurn());
                        assertThat(currentTurn.phase()).isEqualTo(TurnPhase.REMOVAL);
                    });
        }

        @Test
        @DisplayName("should set the removed words if hints are case insensitive trimmed identical")
        void removeIdentical() {
            final var provider1 = stateWithSelection.turns().get(1).players().stream()
                                                    .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                                    .findFirst().orElseThrow();
            final var provider2 = stateWithSelection.turns().get(1).players().stream()
                                                    .filter(tp -> tp.roles()
                                                                    .contains(TurnRole.PROVIDER) && !tp.playerId()
                                                                                                       .equals(provider1.playerId()))
                                                    .findFirst().orElseThrow();
            final var hint1 = "foo   ";
            final var hint2 = "  FoO  ";
            richStateOf(stateWithSelection)
                    .execute(hint(provider1.playerId(), hint1))
                    .isValidSatisfying(gameState -> {
                        final var currentTurn = gameState.turns().get(gameState.currentTurn());
                        assertThat(currentTurn.hintsToFilter()).isEmpty();
                    })
                    .execute(hint(provider2.playerId(), hint2))
                    .isValidSatisfying(gameState -> {
                        final var currentTurn = gameState.turns().get(gameState.currentTurn());
                        assertThat(currentTurn.hintsToFilter()).containsExactlyInAnyOrder(hint1, hint2);
                    });
        }
    }
}
