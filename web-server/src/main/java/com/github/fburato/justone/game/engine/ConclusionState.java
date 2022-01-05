package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.github.fburato.justone.game.engine.Engine.hostOrRoot;

class ConclusionState implements EngineState {

    @Override
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        final var turn = gameState.turns().get(gameState.currentTurn());
        if (turn.phase() != TurnPhase.CONCLUSION) {
            return Try.failure(new InvalidStateException(ErrorCode.UNEXPECTED_TURN_PHASE));
        }
        if (action.playerAction() != TurnAction.PROCEED) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        if (!hostOrRoot(action.playerId(), gameState)) {
            return Try.failure(new IllegalActionException(ErrorCode.UNAUTHORISED_ACTION));
        }
        if (gameState.currentTurn() < gameState.wordsToGuess().size() - 1) {
            return handleNonTerminal(gameState);
        }
        return handleTerminal(gameState);
    }

    private Try<GameState> handleNonTerminal(GameState gameState) {
        final var turns = new ArrayList<>(gameState.turns());
        final var currentTurn = gameState.currentTurn();
        final var previousGuesser = gameState.turns().get(currentTurn)
                                             .players().stream()
                                             .filter(tp -> tp.roles().contains(TurnRole.GUESSER))
                                             .findFirst()
                                             .orElseThrow();
        final var previousGuesserIndexInPlayers = IntStream.range(0, gameState.players().size())
                                                           .filter(i -> gameState.players().get(i).id()
                                                                                 .equals(previousGuesser.playerId()))
                                                           .findFirst()
                                                           .orElseThrow();
        final var nextGuesserInPlayers = (previousGuesserIndexInPlayers + 1) % gameState.players().size();
        final var nextRemoverInPlayers = (nextGuesserInPlayers + 1) % gameState.players().size();
        final var players = IntStream.range(0, gameState.players().size())
                                     .mapToObj(i -> {
                                         if (i == nextGuesserInPlayers) {
                                             return new TurnPlayer(gameState.players().get(i).id(),
                                                                   List.of(TurnRole.GUESSER));
                                         } else if (i == nextRemoverInPlayers) {
                                             return new TurnPlayer(gameState.players().get(i).id(),
                                                                   List.of(TurnRole.REMOVER, TurnRole.PROVIDER));
                                         } else {
                                             return new TurnPlayer(gameState.players().get(i).id(),
                                                                   List.of(TurnRole.PROVIDER));
                                         }
                                     }).toList();
        turns.add(new Turn(
                TurnPhase.SELECTION,
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                players
        ));
        return Try.success(new GameState(
                gameState.id(),
                GameStatus.IN_PROGRESS,
                gameState.players(),
                turns,
                gameState.wordsToGuess(),
                currentTurn + 1
        ));
    }

    private Try<GameState> handleTerminal(GameState gameState) {
        return Try.success(new GameState(
                gameState.id(),
                GameStatus.CONCLUDED,
                gameState.players(),
                gameState.turns(),
                gameState.wordsToGuess(),
                gameState.currentTurn() + 1
        ));
    }

}
