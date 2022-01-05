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

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

class GuessingState implements EngineState {

    @Override
    @SuppressWarnings("unchecked")
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        if (turn.phase() != TurnPhase.GUESSING) {
            return Try.failure(new InvalidStateException(ErrorCode.UNEXPECTED_TURN_PHASE));
        }
        if (action.playerAction() != TurnAction.GUESS_WORD) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        final var guessers = turn.players().stream()
                                 .filter(tp -> tp.roles().contains(TurnRole.GUESSER))
                                 .map(TurnPlayer::playerId)
                                 .collect(Collectors.toSet());
        if (!guessers.contains(action.playerId())) {
            return Try.failure(new IllegalActionException(ErrorCode.UNAUTHORISED_ACTION));
        }
        return handleGuess(gameState, action.playerId(), ((Action<String>) action).payload());
    }

    private Try<GameState> handleGuess(GameState gameState, String guesser, String guess) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        final var turns = new ArrayList<>(gameState.turns());
        turns.set(gameState.currentTurn(),
                  new Turn(TurnPhase.CONCLUSION,
                           turn.providedHints(),
                           turn.hintsToFilter(),
                           turn.hintsToRemove(),
                           Optional.of(new PlayerWord(guesser, guess)),
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
