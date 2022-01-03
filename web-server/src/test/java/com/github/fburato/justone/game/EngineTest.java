package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.InvalidActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;
import static com.github.fburato.justone.game.EngineTestUtils.RichState;
import static com.github.fburato.justone.game.EngineTestUtils.admit;
import static com.github.fburato.justone.game.EngineTestUtils.cancel;
import static com.github.fburato.justone.game.EngineTestUtils.extractGuesser;
import static com.github.fburato.justone.game.EngineTestUtils.extractProviders;
import static com.github.fburato.justone.game.EngineTestUtils.extractRemover;
import static com.github.fburato.justone.game.EngineTestUtils.guessWord;
import static com.github.fburato.justone.game.EngineTestUtils.hint;
import static com.github.fburato.justone.game.EngineTestUtils.kick;
import static com.github.fburato.justone.game.EngineTestUtils.proceed;
import static com.github.fburato.justone.game.EngineTestUtils.removeProvided;
import static com.github.fburato.justone.model.Builders.gameStateBuilder;
import static com.github.fburato.justone.utils.StreamUtils.append;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EngineTest {

    private final String id = randomString();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());


    private final ActionCompiler actionCompiler = mock(ActionCompiler.class);
    private final Engine testee = new Engine(actionCompiler);

    private final RichState state = new RichState(testee.init(id, host, players, wordsToGuess),
                                                  testee::execute).isValid();

    @Test
    @DisplayName("should validate the action with the validator")
    void validateAction() {
        when(actionCompiler.compile(any())).then(a -> Try.success(a.getArgument(0, Action.class)));
        final GameState state = testee.init(id, host, players, wordsToGuess).get();
        final var action = new Action<>(randomString(), TurnAction.PROCEED, Void.class, null);

        testee.execute(state, action);

        verify(actionCompiler).compile(action);
    }

    @Test
    @DisplayName("should fail if validator fails")
    void failOnValidationFailure() {
        final var exception = new RuntimeException(randomString());
        when(actionCompiler.compile(any())).then(a -> Try.failure(exception));
        final GameState state = testee.init(id, host, players, wordsToGuess).get();
        final var action = new Action<>(randomString(), TurnAction.PROCEED, Void.class, null);

        final var tryState = testee.execute(state, action);

        assertThat(tryState.isFailure()).isTrue();
        assertThat(tryState.getCause()).isEqualTo(exception);
    }

    @Test
    @DisplayName("should fail with UNRECOGNISED_STATE if state is not recognised")
    void unrecognisedState() {
        when(actionCompiler.compile(any())).then(a -> Try.success(a.getArgument(0, Action.class)));
        final var initState = testee.init(id, host, players, wordsToGuess).get();
        final var invalidState = new GameState(
                initState.id(),
                initState.status(),
                initState.players(),
                List.of(new Turn(null, List.of(), List.of(), List.of(), Optional.empty(), List.of())),
                initState.wordsToGuess(),
                initState.currentTurn()
        );
        new RichState(Try.success(invalidState), testee::execute)
                .execute(proceed(host))
                .isInvalidInstanceOfSatisfying(InvalidStateException.class, failure ->
                        assertThat(failure.errorCodes())
                                .containsExactly(
                                        ErrorCode.UNRECOGNISED_STATE));
    }

    void validateAll() {
        when(actionCompiler.compile(any())).then(a -> Try.success(a.getArgument(0, Action.class)));
    }

    @Test
    @DisplayName("on cancel, should mark the game as cancelled")
    void cancelGameCancelled() {
        validateAll();
        final var initialState = state.gameState().get();

        state.execute(cancel(host))
             .isValidSatisfying(gameState ->
                                        assertThat(gameState).isEqualTo(new GameState(
                                                initialState.id(),
                                                GameStatus.CANCELLED,
                                                initialState.players(),
                                                initialState.turns(),
                                                initialState.wordsToGuess(),
                                                initialState.currentTurn()
                                        )));
    }

    @Test
    @DisplayName("should fail if currentTurn is negative")
    void failOnNegativeCurrentTurn() {
        validateAll();

        final var firstTurnState = state.execute(proceed(host)).gameState().get();
        final var invalidState = new RichState(Try.success(gameStateBuilder(firstTurnState)
                                                                   .with(gsb -> gsb.currentTurn = -1).build()),
                                               testee::execute);
        invalidState.execute(proceed(host))
                    .isInvalidInstanceOfSatisfying(InvalidStateException.class, ise ->
                            assertThat(ise.errorCodes()).containsExactly(ErrorCode.INVALID_CURRENT_TURN));
    }

    @Test
    @DisplayName("should fail if currentTurn is greater than size")
    void failOnGreaterThanSizeCurrentTurn() {
        validateAll();

        final var firstTurnState = state.execute(proceed(host)).gameState().get();
        final var invalidState = new RichState(Try.success(gameStateBuilder(firstTurnState)
                                                                   .with(gsb -> gsb.currentTurn = 5).build()),
                                               testee::execute);
        invalidState.execute(proceed(host))
                    .isInvalidInstanceOfSatisfying(InvalidStateException.class, ise ->
                            assertThat(ise.errorCodes()).containsExactly(ErrorCode.INVALID_CURRENT_TURN));
    }

    @Test
    @DisplayName("should allow to provide hints on selection")
    void provideHints() {
        validateAll();

        final var firstTurnState = state.execute(proceed(host))
                                        .isValid();
        final var providers = extractProviders(firstTurnState.gameState().get().turns().get(0));
        firstTurnState.execute(hint(providers.get(0), randomString()))
                      .isValid();
    }

    @Test
    @DisplayName("should allow to remove hints after selection")
    void removeHints() {
        validateAll();

        final var firstTurnState = state.execute(proceed(host))
                                        .isValid();
        final var hints = List.of(randomString(), randomString());
        final var providers = extractProviders(firstTurnState.gameState().get().turns().get(0));
        final var remover = extractRemover(firstTurnState.gameState().get().turns().get(0));
        firstTurnState
                .execute(hint(providers.get(0), hints.get(0)))
                .execute(hint(providers.get(1), hints.get(1)))
                .execute(removeProvided(remover, hints.get(0)))
                .isValid();
    }

    @Test
    @DisplayName("should allow to guess word after removal")
    void guessWordTest() {
        validateAll();

        final var firstTurnState = state.execute(proceed(host))
                                        .isValid();
        final var hints = List.of(randomString(), randomString());
        final var providers = extractProviders(firstTurnState.gameState().get().turns().get(0));
        final var remover = extractRemover(firstTurnState.gameState().get().turns().get(0));
        final var guesser = extractGuesser(firstTurnState.gameState().get().turns().get(0));
        firstTurnState
                .execute(hint(providers.get(0), hints.get(0)))
                .execute(hint(providers.get(1), hints.get(1)))
                .execute(removeProvided(remover, hints.get(0)))
                .execute(proceed(remover))
                .execute(guessWord(guesser, randomString()))
                .isValid();
    }

    @Nested
    @DisplayName("on init")
    class InitTests {
        @Test
        @DisplayName("should fail with NO_HOST if hostPlayerId is null")
        void noHostOnMissing() {
            final var tryState = testee.init(id, null, players, wordsToGuess);

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NO_HOST));
        }

        @Test
        @DisplayName("should fail with NOT_ENOUGH_PLAYERS playerIds is null")
        void nullPlayerIds() {
            final var tryState = testee.init(id, host, null, wordsToGuess);

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NOT_ENOUGH_PLAYERS));
        }

        @Test
        @DisplayName("should fail with NOT_ENOUGH_PLAYERS if playerIds is empty")
        void emptyPlayerIds() {
            final var tryState = testee.init(id, host, List.of(), wordsToGuess);

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NOT_ENOUGH_PLAYERS));
        }

        @Test
        @DisplayName("should fail with NO_ID if id is null")
        void nullId() {
            final var tryState = testee.init(null, host, players, wordsToGuess);

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NO_ID));

        }


        @Test
        @DisplayName("should fail with NOT_ENOUGH_WORDS if wordsToGuess is null")
        void nullWordsToGuess() {
            final var tryState = testee.init(id, host, players, null);

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NOT_ENOUGH_WORDS));

        }

        @Test
        @DisplayName("should fail with NOT_ENOUGH_WORDS if wordsToGuess is empty")
        void emptyWordsToGuess() {
            final var tryState = testee.init(id, host, players, List.of());

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactly(
                                                    ErrorCode.NOT_ENOUGH_WORDS));

        }

        @Test
        @DisplayName("should fail with multiple error codes if multiple errors present")
        void aggregateErrors() {
            final var tryState = testee.init(null, host, null, List.of());

            assertThat(tryState.isFailure()).isTrue();
            assertThat(tryState.getCause())
                    .isInstanceOfSatisfying(InvalidStateException.class,
                                            exc -> assertThat(exc.errorCodes()).containsExactlyInAnyOrder(
                                                    ErrorCode.NOT_ENOUGH_WORDS,
                                                    ErrorCode.NO_ID,
                                                    ErrorCode.NOT_ENOUGH_PLAYERS));

        }

        @Test
        @DisplayName("on valid input, should return initialised game state")
        void validInitialisedGameState() {
            final var tryState = testee.init(id, host, players, wordsToGuess);

            assertThat(tryState.isFailure()).isFalse();
            assertThat(tryState.get()).isEqualTo(new GameState(
                    id,
                    GameStatus.IN_PROGRESS,
                    Stream.concat(
                            players.stream().map(playerId -> new Player(playerId, PlayerRole.PLAYER)),
                            Stream.of(new Player(host, PlayerRole.HOST))).collect(
                            Collectors.toList()),
                    List.of(),
                    wordsToGuess,
                    0));
        }

        @Test
        @DisplayName("host in playerIds should be a valid game")
        void hostInPlayers() {
            final var playersWithHost = new ArrayList<>(players);
            playersWithHost.add(host);
            final var tryState = testee.init(id, host, playersWithHost, wordsToGuess);

            assertThat(tryState.isFailure()).isFalse();
            assertThat(tryState.get()).isEqualTo(new GameState(
                    id,
                    GameStatus.IN_PROGRESS,
                    Stream.concat(
                            players.stream().map(playerId -> new Player(playerId, PlayerRole.PLAYER)),
                            Stream.of(new Player(host, PlayerRole.HOST))).collect(
                            Collectors.toList()),
                    List.of(),
                    wordsToGuess,
                    0));
        }
    }

    @Nested
    @DisplayName("on any state")
    class InitStateTests {

        @BeforeEach
        void setUp() {
            validateAll();
        }

        @Test
        @DisplayName("should reject action of non player")
        void rejectNonPlayer() {
            state.execute(proceed(randomString()))
                 .isInvalid();
        }

        @Test
        @DisplayName("should allow to cancel the game as host")
        void cancelGameHost() {
            state.execute(cancel(host))
                 .isValid();
        }

        @Test
        @DisplayName("should allow to cancel the game as root")
        void cancelGameRoot() {
            state.execute(cancel("root"))
                 .isValid();
        }

        @Test
        @DisplayName("should not allow to cancel the game as another player")
        void noCancelGameAsNonHost() {
            state.execute(cancel(players.get(0)))
                 .isInvalid();
        }

        @Test
        @DisplayName("should allow to kick player as host")
        void kickAsHost() {
            state.execute(kick(host, randomString()))
                 .isValid();
        }

        @Test
        @DisplayName("should allow to admit player as host")
        void admitAsHost() {
            state.execute(admit(host, randomString()))
                 .isValid();
        }

        @Test
        @DisplayName("should allow to kick player as root")
        void kickAsRoot() {
            state.execute(kick("root", randomString()))
                 .isValid();
        }

        @Test
        @DisplayName("should allow to admit player as root")
        void admitAsRoot() {
            state.execute(admit("root", randomString()))
                 .isValid();
        }

        @Test
        @DisplayName("should not allow to kick player as player")
        void noKickAsPlayer() {
            state.execute(kick(players.get(0), randomString()))
                 .isInvalid();
        }

        @Test
        @DisplayName("should not allow to admit player as player")
        void noAdmitAsPlayer() {
            state.execute(admit(players.get(0), randomString()))
                 .isInvalid();
        }
    }

    @Nested
    @DisplayName("on admit")
    class AdmitTests {

        @Test
        @DisplayName("should add the player to the list of players in the game")
        void appendPlayerOnAdmit() {
            validateAll();
            final var stateBeforeAction = state.gameState().get();
            final var newPlayerId = randomString();

            state.execute(admit(host, newPlayerId))
                 .isValidSatisfying(gameState ->
                                            assertThat(gameState).isEqualTo(new GameState(
                                                    stateBeforeAction.id(),
                                                    stateBeforeAction.status(),
                                                    append(stateBeforeAction.players().stream(),
                                                           new Player(newPlayerId, PlayerRole.PLAYER)).toList(),
                                                    stateBeforeAction.turns(),
                                                    stateBeforeAction.wordsToGuess(),
                                                    stateBeforeAction.currentTurn()
                                            )));
        }

        @Test
        @DisplayName("should return INVALID_PAYLOAD if playerId already exists")
        void test() {
            validateAll();

            state.execute(admit(host, players.get(0)))
                 .isInvalidInstanceOfSatisfying(InvalidActionException.class, failure ->
                         assertThat(failure.errorCodes()).containsExactly(ErrorCode.ILLEGAL_ACTION));
        }
    }
}
