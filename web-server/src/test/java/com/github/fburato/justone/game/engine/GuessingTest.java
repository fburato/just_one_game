package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.PlayerWord;
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
import static com.github.fburato.justone.game.engine.EngineTestUtils.guessWord;
import static com.github.fburato.justone.game.engine.EngineTestUtils.proceed;
import static com.github.fburato.justone.model.Builders.gameStateBuilder;
import static com.github.fburato.justone.model.Builders.turnBuilder;
import static com.github.fburato.justone.utils.StreamUtils.append;
import static org.assertj.core.api.Assertions.assertThat;

class GuessingTest {

    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());
    private final Engine engine = new Engine(Try::success);
    private final EngineTestUtils.RichState initialState = new EngineTestUtils.RichState(
            engine.init(id, host, players, wordsToGuess), engine::execute)
            .execute(proceed(host));
    private final GameState stateWithGuessing = gameStateBuilder(initialState.gameState().get())
            .with(gsb -> {
                final var turns = initialState.gameState().get().turns();
                gsb.currentTurn = 1;
                gsb.turns = new ArrayList<>(turns);
                gsb.turns.add(turnBuilder(turns.get(0)).with(tb ->
                                                                     tb.phase = TurnPhase.GUESSING
                ).build());
            }).build();

    private final GuessingState testee = new GuessingState();

    private EngineTestUtils.RichState richStateOf(GameState gameState) {
        return new EngineTestUtils.RichState(Try.success(gameState), testee);
    }

    @Test
    @DisplayName("should fail with UNAUTHORISED_ACTION if player is not guesser")
    void failOnNotRemover() {
        final var notGuesser = getNotGuesser(stateWithGuessing);

        richStateOf(stateWithGuessing)
                .execute(guessWord(notGuesser, randomString()))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNAUTHORISED_ACTION));

    }

    private String extractGuesser(GameState gameState) {
        final var currentTurn = gameState.turns().get(gameState.currentTurn());
        return EngineTestUtils.extractGuesser(currentTurn);
    }

    private String getNotGuesser(GameState gameState) {
        final var remover = extractGuesser(gameState);
        return append(players.stream(), host)
                .filter(p -> !remover.equals(p))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("should fail with UNEXPECTED_TURN_PHASE if current turn state is not GUESSING")
    void failOnNotGuessing() {
        final var gameStateInWrongPhase = gameStateBuilder(stateWithGuessing)
                .with(gsb -> {
                    gsb.turns = new ArrayList<>(stateWithGuessing.turns());
                    gsb.turns.set(1, turnBuilder(stateWithGuessing.turns().get(0))
                            .with(tb -> tb.phase = TurnPhase.CONCLUSION).build());
                }).build();
        richStateOf(gameStateInWrongPhase)
                .execute(guessWord(extractGuesser(stateWithGuessing), randomString()))
                .isInvalidInstanceOfSatisfying(InvalidStateException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.UNEXPECTED_TURN_PHASE));

    }

    @Test
    @DisplayName("should accept action GUESS_WORD")
    void acceptGuessWord() {
        final var guesser = extractGuesser(stateWithGuessing);
        richStateOf(stateWithGuessing)
                .execute(guessWord(guesser, randomString()))
                .isValid();
    }

    @ParameterizedTest
    @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"GUESS_WORD"})
    @DisplayName("should fail with ILLEGAL_ACTION with other actions")
    void failOnOtherActions(TurnAction turnAction) {
        final var remover = extractGuesser(stateWithGuessing);

        richStateOf(stateWithGuessing)
                .execute(new Action<>(remover, turnAction, Object.class, null))
                .isInvalidInstanceOfSatisfying(IllegalActionException.class, iae ->
                        assertThat(iae.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));

    }

    @Nested
    @DisplayName("on GUESS_WORD")
    class GuessWordTest {

        private final String guesser = extractGuesser(stateWithGuessing);

        @Test
        @DisplayName("should set the guessed word")
        void setGuessed() {
            final var guessed = randomString();
            richStateOf(stateWithGuessing)
                    .execute(guessWord(guesser, guessed))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());

                        assertThat(turn.wordGuessed()).contains(new PlayerWord(guesser, guessed));
                    });
        }

        @Test
        @DisplayName("should transition the turn phase to CONCLUSION")
        void transitionToConclusion() {
            richStateOf(stateWithGuessing)
                    .execute(guessWord(guesser, randomString()))
                    .isValidSatisfying(gameState -> {
                        final var turn = gameState.turns().get(gameState.currentTurn());

                        assertThat(turn.phase()).isEqualTo(TurnPhase.CONCLUSION);
                    });
        }
    }
}
