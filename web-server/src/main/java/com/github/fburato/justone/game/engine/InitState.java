package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.github.fburato.justone.game.engine.Engine.hostOrRoot;

class InitState implements EngineState {

    @Override
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        if (action.playerAction() == TurnAction.PROCEED
                && hostOrRoot(action.playerId(), gameState)) {
            final var guesserIndex = RandomUtils.nextInt(0, gameState.players().size());
            final var removerIndex = (guesserIndex + 1) % gameState.players().size();
            final var players = IntStream.range(0, gameState.players().size())
                                         .mapToObj(i -> {
                                             final var player = gameState.players().get(i);
                                             final List<TurnRole> roles;
                                             if (i == guesserIndex) {
                                                 roles = List.of(TurnRole.GUESSER);
                                             } else if (i == removerIndex) {
                                                 roles = List.of(TurnRole.REMOVER, TurnRole.PROVIDER);
                                             } else {
                                                 roles = List.of(TurnRole.PROVIDER);
                                             }
                                             return new TurnPlayer(player.id(), roles);
                                         })
                                         .toList();
            final var firstTurn = new Turn(TurnPhase.SELECTION,
                                           List.of(),
                                           List.of(),
                                           List.of(),
                                           Optional.empty(),
                                           players);
            return Try.success(new GameState(gameState.id(),
                                             GameStatus.IN_PROGRESS,
                                             gameState.players(),
                                             List.of(firstTurn),
                                             gameState.wordsToGuess(),
                                             0));
        }
        return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
    }
}
