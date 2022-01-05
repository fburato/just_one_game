package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnPhase;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.github.fburato.justone.game.engine.Engine.hostOrRoot;

class KickState implements EngineState {

    @Override
    @SuppressWarnings("unchecked")
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        final Action<String> kickAction = (Action<String>) action;
        if (!hostOrRoot(kickAction.playerId(), gameState)) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        final var playerId = kickAction.payload();
        if (gameState.players().stream()
                     .noneMatch(p -> StringUtils.equals(p.id(), playerId))) {
            return Try.success(gameState);
        }
        final var playerIndexToRemove = IntStream.range(0, gameState.players().size())
                                                 .filter(i -> StringUtils.equals(gameState.players().get(i).id(),
                                                                                 playerId))
                                                 .findFirst().orElseThrow();
        final var newPlayers = new ArrayList<>(gameState.players());
        if (gameState.players().size() - 1 < 2) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        if (gameState.players().get(playerIndexToRemove).playerRole() == PlayerRole.HOST) {
            final var nextHostIndex = (playerIndexToRemove + 1) % gameState.players().size();
            final var nextHost = gameState.players().get(nextHostIndex);
            newPlayers.set(nextHostIndex, new Player(nextHost.id(), PlayerRole.HOST));
        }
        newPlayers.remove(playerIndexToRemove);
        final List<Turn> turns;
        if (gameState.currentTurn() < gameState.turns().size()) {
            final var currentTurn = gameState.turns().get(gameState.currentTurn());
            turns = new ArrayList<>(gameState.turns());
            turns.set(gameState.currentTurn(), new Turn(
                    TurnPhase.CONCLUSION,
                    currentTurn.providedHints(),
                    currentTurn.hintsToFilter(),
                    currentTurn.hintsToRemove(),
                    Optional.of(new PlayerWord(Engine.ROOT, "")),
                    currentTurn.players().stream()
                               .filter(turnPlayer -> !StringUtils.equals(turnPlayer.playerId(), playerId))
                               .toList()
            ));
        } else {
            turns = gameState.turns();
        }
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                newPlayers,
                turns,
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }
}
