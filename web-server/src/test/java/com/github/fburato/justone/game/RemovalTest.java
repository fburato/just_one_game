package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.EngineTestUtils.cancelRemoval;
import static com.github.fburato.justone.game.EngineTestUtils.proceed;
import static com.github.fburato.justone.game.EngineTestUtils.removeProvided;
import static com.github.fburato.justone.model.Builders.gameStateBuilder;
import static com.github.fburato.justone.model.Builders.turnBuilder;
import static com.github.fburato.justone.utils.StreamUtils.append;
import static org.assertj.core.api.Assertions.assertThat;

class RemovalTest {
    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());
    private final List<String> providedHints = List.of(randomString(), randomString(), randomString());
    private final Engine engine = new Engine(Try::success);
    private final EngineTestUtils.RichState initialState = new EngineTestUtils.RichState(
            engine.init(id, host, players, wordsToGuess), engine::execute)
            .execute(proceed(host));
    private final GameState stateWithRemoval = gameStateBuilder(initialState.gameState().get())
            .with(gsb -> {
                final var turns = initialState.gameState().get().turns();
                gsb.currentTurn = 1;
                gsb.turns = new ArrayList<>(turns);
                gsb.turns.add(turnBuilder(turns.get(0)).with(tb -> {
                    tb.phase = TurnPhase.REMOVAL;
                    tb.providedHints = providedHints.stream()
                                                    .map(s -> new PlayerWord(randomString(), s))
                                                    .toList();
                    tb.hintsToFilter = List.of(providedHints.get(0));
                }).build());
            }).build();

    private final RemovalState testee = new RemovalState();

    private EngineTestUtils.RichState richStateOf(GameState gameState) {
        return new EngineTestUtils.RichState(Try.success(gameState), testee);
    }

    @Test
    @DisplayName("should fail with UNAUTHORISED_ACTION if player is not remover")
    void failOnNotRemover() {
        final var notRemover = getNotRemover(stateWithRemoval);

        richStateOf(stateWithRemoval)
                .execute(removeProvided(notRemover, randomString()))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNAUTHORISED_ACTION));
    }

    private String extractRemover(GameState gameState) {
        final var currentTurn = gameState.turns().get(gameState.currentTurn());
        return EngineTestUtils.extractRemover(currentTurn);
    }

    private String getNotRemover(GameState gameState) {
        final var remover = extractRemover(gameState);
        return append(players.stream(), host)
                .filter(p -> !remover.equals(p))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("should fail with UNEXPECTED_TURN_PHASE if current turn state is not REMOVAL")
    void failOnNotRemoval() {
        final var gameStateInWrongPhase = gameStateBuilder(stateWithRemoval)
                .with(gsb -> {
                    gsb.turns = new ArrayList<>(stateWithRemoval.turns());
                    gsb.turns.set(1, turnBuilder(stateWithRemoval.turns().get(0))
                            .with(tb -> tb.phase = TurnPhase.CONCLUSION).build());
                }).build();
        richStateOf(gameStateInWrongPhase)
                .execute(removeProvided(host, randomString()))
                .isInvalidInstanceOfSatisfying(InvalidStateException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNEXPECTED_TURN_PHASE));
    }

    @Test
    @DisplayName("should accept action PROCEED")
    void acceptProceed() {
        final var remover = extractRemover(stateWithRemoval);
        richStateOf(stateWithRemoval)
                .execute(proceed(remover))
                .isValid();
    }

    @Test
    @DisplayName("should accept action REMOVE_HINT")
    void acceptRemoveHint() {
        final var remover = extractRemover(stateWithRemoval);
        richStateOf(stateWithRemoval)
                .execute(removeProvided(remover, randomString()))
                .isValid();
    }

    @Test
    @DisplayName("should accept action CANCEL_REMOVED_HINT")
    void acceptCancelRemovedHint() {
        final var remover = extractRemover(stateWithRemoval);
        richStateOf(stateWithRemoval)
                .execute(cancelRemoval(remover, randomString()))
                .isValid();
    }

    @ParameterizedTest
    @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"REMOVE_HINT", "CANCEL_REMOVED_HINT", "PROCEED"})
    @DisplayName("should fail with ILLEGAL_ACTION with other actions")
    void failOnOtherActions(TurnAction turnAction) {
        final var remover = extractRemover(stateWithRemoval);

        richStateOf(stateWithRemoval)
                .execute(new Action<>(remover, turnAction, Object.class, null))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));
    }

    @Nested
    @DisplayName("on REMOVE_HINT")
    class RemoveHintTests {
        final String remover = extractRemover(stateWithRemoval);
        final Turn turnBeforeAction = stateWithRemoval.turns().get(stateWithRemoval.currentTurn());

        @Test
        @DisplayName("should do nothing if hint is not a provided one")
        void doNothingOnNotProvided() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, randomString()))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.turns().get(gameState.currentTurn()))
                                                       .isEqualTo(turnBeforeAction));
        }

        @Test
        @DisplayName("should do nothing if hint is a provided one which was filtered")
        void doNothingOnProvidedFiltered() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(0)))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.turns().get(gameState.currentTurn()))
                                                       .isEqualTo(turnBeforeAction));
        }

        @Test
        @DisplayName("should mark as removed hint provided that are removed")
        void removeProvidedTest() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactly(new PlayerWord(remover, providedHints.get(1)));
                    });
        }

        @Test
        @DisplayName("should mark as removed multiple hint provided that are removed")
        void removeMultipleProvidedTest() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactly(new PlayerWord(remover, providedHints.get(1)));
                    })
                    .execute(removeProvided(remover, providedHints.get(2)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactlyInAnyOrder(
                                new PlayerWord(remover, providedHints.get(2)),
                                new PlayerWord(remover, providedHints.get(1)));
                    });
        }

        @Test
        @DisplayName("should keep hint as removed hint provided if it was already removed")
        void removeAlreadyRemovedTest() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactly(new PlayerWord(remover, providedHints.get(1)));
                    })
                    .execute(removeProvided(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactly(new PlayerWord(remover, providedHints.get(1)));
                    });
        }
    }

    @Nested
    @DisplayName("on CANCEL_REMOVED_HINT")
    class CancelRemoveHintTest {

        final String remover = extractRemover(stateWithRemoval);
        final Turn turnBeforeAction = stateWithRemoval.turns().get(stateWithRemoval.currentTurn());

        @Test
        @DisplayName("should do nothing if hint was not removed")
        void doNothingOnNotRemoved() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(0)))
                    .execute(cancelRemoval(remover, providedHints.get(0)))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.turns().get(gameState.currentTurn()))
                                                       .isEqualTo(turnBeforeAction));
        }

        @Test
        @DisplayName("should remove cancelled hint from removed")
        void deleteFromRemoved() {
            richStateOf(stateWithRemoval)
                    .execute(removeProvided(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.hintsToRemove()).containsExactly(new PlayerWord(remover, providedHints.get(1)));
                    })
                    .execute(cancelRemoval(remover, providedHints.get(1)))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.turns().get(gameState.currentTurn()))
                                                       .isEqualTo(turnBeforeAction));
        }
    }

    @Nested
    @DisplayName("on PROCEED")
    class ProceedTest {
        @Test
        @DisplayName("should transition current turn to GUESSING")
        void transitionToGuessing() {

            final String remover = extractRemover(stateWithRemoval);
            richStateOf(stateWithRemoval)
                    .execute(proceed(remover))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        assertThat(turn.phase()).isEqualTo(TurnPhase.GUESSING);
                    });
        }
    }
}
