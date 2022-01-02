package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;
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
    private final List<String> allPlayers = Stream.concat(Stream.of(host), players.stream()).toList();

    private final ActionCompiler actionCompiler = mock(ActionCompiler.class);
    private final Engine testee = new Engine(actionCompiler);
    private final Action<Void> hostProceed = new Action<>(host, TurnAction.PROCEED, Void.class, null);
    private final Action<Void> rootProceed = new Action<>("root", TurnAction.PROCEED, Void.class, null);
    private final Action<Void> hostCancel = new Action<>(host, TurnAction.CANCEL_GAME, Void.class, null);
    private final Action<Void> rootCancel = new Action<>("root", TurnAction.CANCEL_GAME, Void.class, null);
    private final Action<Void> invalidProceed = new Action<>(randomString(), TurnAction.PROCEED, Void.class, null);
    private final Action<Void> nonHostProceed = new Action<>(players.get(0), TurnAction.PROCEED, Void.class, null);

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

    public String extractGuesser(Turn turn) {
        return extractOneRole(turn, TurnRole.GUESSER);
    }

    public String extractOneRole(Turn turn, TurnRole turnRole) {
        final var candidates = turn.players().stream()
                                   .filter(p -> p.roles().contains(turnRole))
                                   .toList();
        assertThat(candidates.size()).withFailMessage("expected turn=%s to have one player with role=%s", turn,
                                                      turnRole)
                                     .isOne();
        return candidates.get(0).playerId();
    }

    public List<String> extractProviders(Turn turn) {
        return turn.players().stream()
                   .filter(p -> p.roles().contains(TurnRole.PROVIDER))
                   .map(TurnPlayer::playerId)
                   .toList();

    }

    public String extractRemover(Turn turn) {
        return extractOneRole(turn, TurnRole.REMOVER);
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
    @DisplayName("on init state")
    class InitStateTests {

        private final RichState state = new RichState(testee.init(id, host, players, wordsToGuess), testee).isValid();

        @BeforeEach
        void compilePassThrough() {
            when(actionCompiler.compile(any())).then(a -> Try.success(a.getArgument(0, Action.class)));

        }

        @Test
        @DisplayName("should allow to proceed with host")
        void proceedHost() {
            state.execute(hostProceed)
                 .isValid();
        }

        @Test
        @DisplayName("should allow to proceed with root")
        void proceedRoot() {
            state.execute(rootProceed)
                 .isValid();
        }

        @Test
        @DisplayName("should not allow to proceed with non host")
        void proceedNonHost() {
            state.execute(nonHostProceed)
                 .isFailed();
        }

        @Test
        @DisplayName("should reject action of non player")
        void rejectNonPlayer() {
            state.execute(invalidProceed)
                 .isFailed();
        }

        @Test
        @DisplayName("on proceed, should initialise turn 0 with SELECTION phase")
        void turn0WithSelection() {
            state.execute(hostProceed)
                 .isValidSatisfying(gameState -> {
                     assertThat(gameState.turns().size()).isOne();
                     final var turn0 = gameState.turns().get(0);
                     assertThat(turn0.phase()).isEqualTo(TurnPhase.SELECTION);
                 });
        }

        @Test
        @DisplayName("on proceed, should have all words empty")
        void turn0WithAllEmpty() {
            state.execute(hostProceed)
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
            state.execute(hostProceed)
                 .isValidSatisfying(gameState -> {
                     final var turn0 = gameState.turns().get(0);
                     final var guesser = extractGuesser(turn0);

                     assertThat(allPlayers).contains(guesser);
                 });
        }

        @Test
        @DisplayName("on proceed, should select only one of the existing players as REMOVER")
        void turn0WithOneRemover() {
            state.execute(hostProceed)
                 .isValidSatisfying(gameState -> {
                     final var turn0 = gameState.turns().get(0);
                     final var remover = extractRemover(turn0);

                     assertThat(allPlayers).contains(remover);
                 });
        }

        @Test
        @DisplayName("on proceed, should not select the same player as guesser and remover")
        void turn0DifferentGuesserRemover() {
            state.execute(hostProceed)
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
            state.execute(hostProceed)
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

        @Test
        @DisplayName("should allow to cancel the game as host")
        void cancelGameHost() {
            state.execute(hostCancel)
                 .isValid();
        }

        @Test
        @DisplayName("should allow to cancel the game as root")
        void cancelGameRoot() {
            state.execute(rootCancel)
                 .isValid();
        }

        @Test
        @DisplayName("should not allow to cancel the game as another player")
        void noCancelGameAsNonHost() {
            state.execute(new Action<>(players.get(0), TurnAction.CANCEL_GAME, Void.class, null))
                 .isFailed();
        }

        @Test
        @DisplayName("on cancel, should mark the game as cancelled")
        void cancelGameCancelled() {
            state.execute(hostCancel)
                 .isValidSatisfying(gameState -> {
                     assertThat(gameState.status()).isEqualTo(GameStatus.CANCELLED);
                     assertThat(gameState.turns()).isEmpty();
                 });
        }

        @ParameterizedTest
        @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROCEED", "CANCEL_GAME"})
        @DisplayName("should reject actions not allowed")
        void rejectActions(TurnAction turnAction) {
            final var action = new Action<>(host, turnAction, String.class, randomString());
            state.execute(action)
                 .isInvalidSatisfying(failure ->
                                              assertThat(failure).isInstanceOfSatisfying(IllegalActionException.class,
                                                                                         iae -> assertThat(
                                                                                                 iae.errorCodes())
                                                                                                 .containsExactly(
                                                                                                         ErrorCode.ILLEGAL_ACTION)));
        }
    }
}

class RichState {
    private final Try<GameState> gameState;
    private final Engine engine;

    public RichState(Try<GameState> gameState, Engine engine) {
        this.gameState = gameState;
        this.engine = engine;
    }

    public RichState isValid() {
        assertThat(gameState.isFailure()).isFalse();
        return this;
    }

    public RichState isFailed() {
        assertThat(gameState.isFailure()).isTrue();
        return this;
    }

    public RichState execute(Action<?> action) {
        final var current = isValid();
        return new RichState(engine.execute(current.gameState.get(), action), engine);
    }

    public RichState isValidSatisfying(Consumer<GameState> assertions) {
        final var state = isValid();
        assertions.accept(state.gameState.get());
        return state;
    }

    public RichState isInvalidSatisfying(Consumer<Throwable> assertions) {
        final var state = isFailed();
        assertions.accept(state.gameState.getCause());
        return state;
    }
}
