package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.engine.EngineTestUtils.extractGuesser;
import static com.github.fburato.justone.game.engine.EngineTestUtils.proceed;
import static com.github.fburato.justone.model.Builders.gameStateBuilder;
import static com.github.fburato.justone.model.Builders.turnBuilder;
import static com.github.fburato.justone.utils.StreamUtils.append;
import static org.assertj.core.api.Assertions.assertThat;

class ConclusionTest {
    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString(), randomString());
    private final Engine engine = new Engine(Try::success);
    private final EngineTestUtils.RichState initialState = new EngineTestUtils.RichState(
            engine.init(id, host, players, wordsToGuess), engine::execute)
            .execute(proceed(host));
    private final GameState stateWithConcluding = gameStateBuilder(initialState.gameState().get())
            .with(gsb -> {
                final var turns = initialState.gameState().get().turns();
                gsb.currentTurn = 1;
                gsb.turns = new ArrayList<>(turns);
                gsb.turns.add(turnBuilder(turns.get(0)).with(tb ->
                                                                     tb.phase = TurnPhase.CONCLUSION
                ).build());
            }).build();
    private final GameState stateWithConcludingAtEnd = gameStateBuilder(stateWithConcluding)
            .with(gsb -> {
                final var turns = stateWithConcluding.turns();
                gsb.currentTurn = 2;
                gsb.turns = new ArrayList<>(turns);
                gsb.turns.add(turnBuilder(turns.get(0)).with(tb ->
                                                                     tb.phase = TurnPhase.CONCLUSION
                ).build());
            }).build();

    private final ConclusionState testee = new ConclusionState();


    private EngineTestUtils.RichState richStateOf(GameState gameState) {
        return new EngineTestUtils.RichState(Try.success(gameState), testee);
    }

    @Test
    @DisplayName("should fail with UNAUTHORISED_ACTION if player is not host or root")
    void failOnNotHostRoot() {
        final var notGuesser = getNotHost();

        richStateOf(stateWithConcluding)
                .execute(proceed(notGuesser))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNAUTHORISED_ACTION));

    }

    private String getNotHost() {
        return append(players.stream(), host)
                .filter(p -> !host.equals(p))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("should fail with UNEXPECTED_TURN_PHASE if current turn state is not CONCLUSION")
    void failOnNotConclusion() {
        final var gameStateInWrongPhase = gameStateBuilder(stateWithConcluding)
                .with(gsb -> {
                    gsb.turns = new ArrayList<>(stateWithConcluding.turns());
                    gsb.turns.set(1, turnBuilder(stateWithConcluding.turns().get(0))
                            .with(tb -> tb.phase = TurnPhase.SELECTION).build());
                }).build();
        richStateOf(gameStateInWrongPhase)
                .execute(proceed(host))
                .isInvalidInstanceOfSatisfying(InvalidStateException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNEXPECTED_TURN_PHASE));

    }

    @Test
    @DisplayName("should accept action PROCEED by host")
    void acceptProceedHost() {
        richStateOf(stateWithConcluding)
                .execute(proceed(host))
                .isValid();
    }

    @Test
    @DisplayName("should accept action PROCEED by root")
    void acceptProceedRoot() {
        richStateOf(stateWithConcluding)
                .execute(proceed("root"))
                .isValid();
    }

    @ParameterizedTest
    @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROCEED"})
    @DisplayName("should fail with ILLEGAL_ACTION with other actions")
    void failOnOtherActions(TurnAction turnAction) {

        richStateOf(stateWithConcluding)
                .execute(new Action<>(host, turnAction, Object.class, null))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));
    }

    @Nested
    @DisplayName("on PROCEED on non-terminal")
    class ProceedOnNonTerminalTest {

        @Test
        @DisplayName("should increase current turn")
        void increaseCurrentTurn() {
            richStateOf(stateWithConcluding)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.currentTurn()).isEqualTo(2));
        }

        @Test
        @DisplayName("should initialise the new turn as empty")
        void emptyNextTurn() {
            richStateOf(stateWithConcluding)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());

                        assertThat(turn.phase()).isEqualTo(TurnPhase.SELECTION);
                        assertThat(turn.providedHints()).isEmpty();
                        assertThat(turn.hintsToRemove()).isEmpty();
                        assertThat(turn.hintsToFilter()).isEmpty();
                        assertThat(turn.wordGuessed()).isEmpty();
                    });
        }

        @Test
        @DisplayName("should set the guesser to be the next player in the list who was not guesser in the previous turn")
        void nextGuesser() {
            richStateOf(stateWithConcluding)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        final var previousGuesser = extractGuesser(gameState.turns().get(gameState.currentTurn() - 1));
                        final var previousGuesserIndexAmongAllPlayers = IntStream.range(0, gameState.players().size())
                                                                                 .filter(i -> gameState.players().get(i)
                                                                                                       .id()
                                                                                                       .equals(previousGuesser))
                                                                                 .findFirst()
                                                                                 .orElseThrow();

                        final var expectedNextGuesser = (previousGuesserIndexAmongAllPlayers + 1) % gameState.players()
                                                                                                             .size();
                        final var guesserInTurn = turn.players().stream()
                                                      .filter(tp -> tp.roles().contains(TurnRole.GUESSER))
                                                      .toList();
                        assertThat(guesserInTurn.size()).isOne();
                        assertThat(guesserInTurn.get(0)).isEqualTo(
                                new TurnPlayer(gameState.players().get(expectedNextGuesser).id(),
                                               List.of(TurnRole.GUESSER))
                        );
                    });

        }

        @Test
        @DisplayName("should set the remover to be the player next to the guesser")
        void nextRemover() {
            richStateOf(stateWithConcluding)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        final var guesserIndexInTurn = IntStream.range(0, turn.players().size())
                                                                .filter(i -> turn.players().get(i).roles()
                                                                                 .contains(TurnRole.GUESSER))
                                                                .findFirst()
                                                                .orElseThrow();
                        final var expectedRemover = (guesserIndexInTurn + 1) % turn.players().size();
                        assertThat(turn.players().get(expectedRemover).roles())
                                .containsExactlyInAnyOrder(TurnRole.REMOVER, TurnRole.PROVIDER);
                    });
        }

        @Test
        @DisplayName("should set every non guesser as provider")
        void playersProviders() {
            richStateOf(stateWithConcluding)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());
                        final var nonGuessers = IntStream.range(0, turn.players().size())
                                                         .filter(i -> !turn.players().get(i).roles()
                                                                           .contains(TurnRole.GUESSER))
                                                         .boxed()
                                                         .toList();
                        nonGuessers.forEach(i ->
                                                    assertThat(turn.players().get(i).roles()).contains(
                                                            TurnRole.PROVIDER));
                    });
        }
    }

    @Nested
    @DisplayName("on PROCEED on terminal")
    class ProceedOnTerminalTest {

        @Test
        @DisplayName("should increase the currentTurn")
        void increaseCurrentTurn() {
            richStateOf(stateWithConcludingAtEnd)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.currentTurn()).isEqualTo(3));

        }

        @Test
        @DisplayName("should mark the game as concluded")
        void markAsConcluded() {
            richStateOf(stateWithConcludingAtEnd)
                    .execute(proceed(host))
                    .isValidSatisfying(gameState ->
                                               assertThat(gameState.status()).isEqualTo(GameStatus.CONCLUDED));

        }
    }
}
