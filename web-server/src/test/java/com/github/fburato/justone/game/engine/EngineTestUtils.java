package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EngineTestUtils {

    static Action<Void> proceed(String playerId) {
        return new Action<>(playerId, TurnAction.PROCEED, Void.class, null);
    }

    static Action<Void> cancel(String playerId) {
        return new Action<>(playerId, TurnAction.CANCEL_GAME, Void.class, null);
    }

    static Action<String> kick(String executor, String playerId) {
        return new Action<>(executor, TurnAction.KICK_PLAYER, String.class, playerId);
    }

    static Action<String> admit(String executor, String playerId) {
        return new Action<>(executor, TurnAction.ADMIT_PLAYER, String.class, playerId);
    }

    static Action<String> hint(String executor, String hint) {
        return new Action<>(executor, TurnAction.PROVIDE_HINT, String.class, hint);
    }

    static Action<Void> removeHint(String executor) {
        return new Action<>(executor, TurnAction.CANCEL_PROVIDED_HINT, Void.class, null);
    }

    static Action<String> removeProvided(String executor, String provided) {
        return new Action<>(executor, TurnAction.REMOVE_HINT, String.class, provided);
    }

    static Action<String> cancelRemoval(String executor, String removed) {
        return new Action<>(executor, TurnAction.CANCEL_REMOVED_HINT, String.class, removed);
    }

    static Action<String> guessWord(String executor, String guess) {
        return new Action<>(executor, TurnAction.GUESS_WORD, String.class, guess);
    }

    static String extractGuesser(Turn turn) {
        return extractOneRole(turn, TurnRole.GUESSER);
    }

    static String extractRemover(Turn turn) {
        return extractOneRole(turn, TurnRole.REMOVER);
    }


    static String extractOneRole(Turn turn, TurnRole turnRole) {
        final var candidates = turn.players().stream()
                                   .filter(p -> p.roles().contains(turnRole))
                                   .toList();
        assertThat(candidates.size()).withFailMessage("expected turn=%s to have one player with role=%s", turn,
                                                      turnRole)
                                     .isOne();
        return candidates.get(0).playerId();
    }

    static List<String> extractProviders(Turn turn) {
        return turn.players().stream()
                   .filter(p -> p.roles().contains(TurnRole.PROVIDER))
                   .map(TurnPlayer::playerId)
                   .toList();

    }

    static class RichState {
        private final Try<GameState> gameState;
        private final EngineState engine;

        public RichState(Try<GameState> gameState, EngineState engine) {
            this.gameState = gameState;
            this.engine = engine;
        }

        public Try<GameState> gameState() {
            return gameState;
        }

        public RichState isValid() {
            assertThat(gameState.isFailure())
                    .withFailMessage("gameState was expected to be valid, but failed")
                    .isFalse();
            return this;
        }

        public RichState isInvalid() {
            assertThat(gameState.isFailure())
                    .withFailMessage("gameState was expected to be invalid, but it succeeded")
                    .isTrue();
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

        public <T extends Throwable> RichState isInvalidInstanceOfSatisfying(Class<T> throwableType, Consumer<T> t) {
            return isInvalidSatisfying(throwable ->
                                               assertThat(throwable).isInstanceOfSatisfying(throwableType, t));
        }

        public RichState isInvalidSatisfying(Consumer<Throwable> assertions) {
            final var state = isInvalid();
            assertions.accept(state.gameState.getCause());
            return state;
        }
    }

}
