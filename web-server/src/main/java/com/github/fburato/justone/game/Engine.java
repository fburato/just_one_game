package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.TurnAction;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Engine {

    private final ActionCompiler actionCompiler;

    public Engine(
            ActionCompiler actionCompiler
    ) {
        this.actionCompiler = actionCompiler;
    }

    public Try<GameState> init(
            String id,
            String hostPlayerId,
            List<String> playerIds,
            List<String> wordsToGuess
    ) {
        final List<ErrorCode> errorCodes = new ArrayList<>();
        if (StringUtils.isBlank(id)) {
            errorCodes.add(ErrorCode.NO_ID);
        }
        if (StringUtils.isBlank(hostPlayerId)) {
            errorCodes.add(ErrorCode.NO_HOST);
        }
        if (playerIds == null || playerIds.isEmpty()) {
            errorCodes.add(ErrorCode.NOT_ENOUGH_PLAYERS);
        }
        if (wordsToGuess == null || wordsToGuess.isEmpty()) {
            errorCodes.add(ErrorCode.NOT_ENOUGH_WORDS);
        }
        if (!errorCodes.isEmpty()) {
            return Try.failure(new InvalidStateException(errorCodes.toArray(ErrorCode[]::new)));
        }
        final var correctPlayers = playerIds.stream()
                                            .filter(pId -> !StringUtils.equals(hostPlayerId, pId))
                                            .map(playerId -> new Player(playerId, PlayerRole.PLAYER))
                                            .collect(Collectors.toCollection(ArrayList::new));
        correctPlayers.add(new Player(hostPlayerId, PlayerRole.HOST));
        return Try.success(new GameState(
                id,
                GameStatus.IN_PROGRESS,
                correctPlayers,
                List.of(),
                wordsToGuess,
                0
        ));
    }

    public Try<GameState> execute(GameState gameState, Action<?> action) {
        return actionCompiler.compile(action)
                             .flatMapTry(validAction -> {
                                 if (!allowedToRunActions(action.playerId(), gameState)) {
                                     return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
                                 }
                                 if (gameState.currentTurn() == 0 && gameState.turns().isEmpty()) {
                                     return executeFromInitialState(gameState, action);
                                 }
                                 return Try.failure(new RuntimeException());
                             });
    }

    private Try<GameState> executeFromInitialState(GameState gameState, Action<?> action) {
        if (action.playerAction() == TurnAction.CANCEL_GAME && allowedToCancelOrProceed(gameState, action)) {
            return Try.success(new GameState(
                    gameState.id(),
                    GameStatus.CANCELLED,
                    gameState.players(),
                    gameState.turns(),
                    gameState.wordsToGuess(),
                    0
            ));
        }
        if (action.playerAction() == TurnAction.PROCEED && allowedToCancelOrProceed(gameState, action)) {
            return Try.success(gameState);
        }
        return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
    }

    private boolean allowedToCancelOrProceed(GameState gameState, Action<?> action) {
        return List.of(host(gameState), "root").contains(action.playerId());
    }

    private boolean allowedToRunActions(String playerId, GameState gameState) {
        return Stream.concat(Stream.of("root"), gameState.players().stream()
                                                         .map(Player::id))
                     .collect(Collectors.toSet())
                     .contains(playerId);
    }

    private String host(GameState gameState) {
        return gameState.players().stream()
                        .filter(p -> p.playerRole() == PlayerRole.HOST)
                        .findFirst()
                        .map(Player::id)
                        .orElseThrow(() -> new IllegalStateException(
                                String.format("gameState for gameId=%s does not contain host", gameState.id())));
    }
}
