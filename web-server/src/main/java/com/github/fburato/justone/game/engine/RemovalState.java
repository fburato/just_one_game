package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RemovalState implements EngineState {

    @Override
    @SuppressWarnings("unchecked")
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        if (turn.phase() != TurnPhase.REMOVAL) {
            return Try.failure(new InvalidStateException(ErrorCode.UNEXPECTED_TURN_PHASE));
        }
        if (!Set.of(TurnAction.REMOVE_HINT, TurnAction.CANCEL_REMOVED_HINT, TurnAction.PROCEED).contains(
                action.playerAction())) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        final var removers = turn.players().stream()
                                 .filter(tp -> tp.roles().contains(TurnRole.REMOVER))
                                 .map(TurnPlayer::playerId)
                                 .collect(Collectors.toSet());
        if (!removers.contains(action.playerId())) {
            return Try.failure(new IllegalActionException(ErrorCode.UNAUTHORISED_ACTION));
        }
        if (action.playerAction() == TurnAction.PROCEED) {
            return handleProceed(gameState);
        }
        if (action.playerAction() == TurnAction.REMOVE_HINT) {
            return handleRemoveHint(gameState, action.playerId(), ((Action<String>) action).payload());
        }
        return handleCancelRemovedHint(gameState, ((Action<String>) action).payload());
    }

    private Try<GameState> handleProceed(GameState gameState) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turns = new ArrayList<>(gameState.turns());
        turns.set(gameState.currentTurn(),
                  new Turn(TurnPhase.GUESSING,
                           turn.providedHints(),
                           turn.hintsToFilter(),
                           turn.hintsToRemove(),
                           turn.wordGuessed(),
                           turn.players()));
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                gameState.players(),
                turns,
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }

    private Try<GameState> handleRemoveHint(GameState gameState, String remover, String toRemove) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turns = new ArrayList<>(gameState.turns());
        final List<PlayerWord> hintsToRemove = new ArrayList<>(turn.hintsToRemove());
        if (turn.providedHints().stream()
                .map(PlayerWord::word)
                .collect(Collectors.toSet()).contains(toRemove) &&
                !hintsToRemove.stream()
                              .map(PlayerWord::word)
                              .collect(Collectors.toSet()).contains(toRemove) &&
                !turn.hintsToFilter().contains(toRemove)) {
            hintsToRemove.add(new PlayerWord(remover, toRemove));
        }
        turns.set(gameState.currentTurn(),
                  new Turn(turn.phase(),
                           turn.providedHints(),
                           turn.hintsToFilter(),
                           hintsToRemove,
                           turn.wordGuessed(),
                           turn.players()));
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                gameState.players(),
                turns,
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }

    private Try<GameState> handleCancelRemovedHint(GameState gameState, String toRemove) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turns = new ArrayList<>(gameState.turns());
        final List<PlayerWord> hintsToRemove;
        if (turn.hintsToRemove().stream()
                .map(PlayerWord::word)
                .collect(Collectors.toSet()).contains(toRemove)) {
            hintsToRemove = turn.hintsToRemove().stream()
                                .filter(pw -> !StringUtils.equals(toRemove, pw.word()))
                                .toList();
        } else {
            hintsToRemove = turn.hintsToRemove();
        }
        turns.set(gameState.currentTurn(),
                  new Turn(turn.phase(),
                           turn.providedHints(),
                           turn.hintsToFilter(),
                           hintsToRemove,
                           turn.wordGuessed(),
                           turn.players()));
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                gameState.players(),
                turns,
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }
}
