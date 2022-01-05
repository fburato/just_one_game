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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SelectionState implements EngineState {

    private static String normalise(String hint) {
        return StringUtils.lowerCase(StringUtils.strip(hint));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        if (turn.phase() != TurnPhase.SELECTION) {
            return Try.failure(new InvalidStateException(ErrorCode.UNEXPECTED_TURN_PHASE));
        }
        if (!Set.of(TurnAction.PROVIDE_HINT, TurnAction.CANCEL_PROVIDED_HINT).contains(action.playerAction())) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        final var providers = turn.players().stream()
                                  .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                  .map(TurnPlayer::playerId)
                                  .collect(Collectors.toSet());
        if (!providers.contains(action.playerId())) {
            return Try.failure(new IllegalActionException(ErrorCode.UNAUTHORISED_ACTION));
        }

        if (action.playerAction() == TurnAction.PROVIDE_HINT) {
            return handleProvidedHint(gameState, (Action<String>) action);
        }

        return handleCancelHint(gameState, (Action<Void>) action);
    }

    private Try<GameState> handleProvidedHint(GameState gameState, Action<String> hintAction) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turnHints = new ArrayList<>(turn.providedHints());
        final var currentPlayerIndex = IntStream.range(0, turnHints.size())
                                                .filter(i -> turnHints.get(i).playerId().equals(hintAction.playerId()))
                                                .findFirst();
        if (currentPlayerIndex.isEmpty()) {
            turnHints.add(new PlayerWord(hintAction.playerId(), hintAction.payload()));
        } else {
            turnHints.set(currentPlayerIndex.getAsInt(), new PlayerWord(hintAction.playerId(), hintAction.payload()));
        }

        final var turns = new ArrayList<>(gameState.turns());
        turns.set(gameState.currentTurn(), updateTurn(turn, turnHints));
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                gameState.players(),
                turns,
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }

    private Turn updateTurn(Turn oldTurn, List<PlayerWord> providedHints) {
        final Map<String, String> hintsOfProviders = providedHints.stream()
                                                                  .collect(Collectors.toMap(PlayerWord::playerId,
                                                                                            PlayerWord::word));
        final Set<String> providers = oldTurn.players().stream()
                                             .filter(tp -> tp.roles().contains(TurnRole.PROVIDER))
                                             .map(TurnPlayer::playerId)
                                             .collect(Collectors.toSet());
        if (!hintsOfProviders.keySet().containsAll(providers)) {
            return new Turn(oldTurn.phase(), providedHints, oldTurn.hintsToFilter(), oldTurn.hintsToRemove(),
                            oldTurn.wordGuessed(), oldTurn.players());
        }

        final Map<String, Integer> countOfDuplicates = new HashMap<>();
        hintsOfProviders.values().forEach(hint -> {
            final var normalised = normalise(hint);
            final var count = countOfDuplicates.getOrDefault(normalised, 0);
            countOfDuplicates.put(normalised, count + 1);
        });
        final List<String> toExclude = hintsOfProviders.values().stream()
                                                       .filter(hint -> {
                                                           final var normalised = normalise(hint);
                                                           return countOfDuplicates.get(normalised) > 1;
                                                       }).distinct()
                                                       .toList();
        return new Turn(TurnPhase.REMOVAL, providedHints, toExclude, oldTurn.hintsToRemove(), oldTurn.wordGuessed(),
                        oldTurn.players());
    }

    private Try<GameState> handleCancelHint(GameState gameState, Action<Void> cancelAction) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turnHints = new ArrayList<>(turn.providedHints());
        final var currentPlayerIndex = IntStream.range(0, turnHints.size())
                                                .filter(i -> turnHints.get(i).playerId()
                                                                      .equals(cancelAction.playerId()))
                                                .findFirst();
        if (currentPlayerIndex.isPresent()) {
            turnHints.remove(currentPlayerIndex.getAsInt());
        }

        final var turns = new ArrayList<>(gameState.turns());
        turns.set(gameState.currentTurn(),
                  new Turn(turn.phase(), turnHints, turn.hintsToFilter(), turn.hintsToRemove(), turn.wordGuessed(),
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
