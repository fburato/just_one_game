package com.github.fburato.justone.game.engine;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.fburato.justone.utils.StreamUtils.append;

public class Engine {

    public static final String ROOT = "root";
    private static final EngineState UNKNOWN_STATE = new UnknownState();
    private final ActionCompiler actionCompiler;
    private final Map<EngineStateType, EngineState> engineStateRegistry = Map.of(
            EngineStateType.INIT, new InitState(),
            EngineStateType.KICK, new KickState(),
            EngineStateType.SELECTION, new SelectionState(),
            EngineStateType.INVALID_CURRENT_TURN,
            (gs, ac) -> Try.failure(new InvalidStateException(ErrorCode.INVALID_CURRENT_TURN)),
            EngineStateType.REMOVAL, new RemovalState(),
            EngineStateType.GUESS, new GuessingState(),
            EngineStateType.CONCLUSION, new ConclusionState()
    );

    public Engine(ActionCompiler actionCompiler) {
        this.actionCompiler = actionCompiler;
    }

    static boolean hostOrRoot(String playerId, GameState gameState) {
        return Set.of(Engine.host(gameState), ROOT).contains(playerId);
    }

    static String host(GameState gameState) {
        return gameState.players().stream()
                        .filter(p -> p.playerRole() == PlayerRole.HOST)
                        .findFirst()
                        .map(Player::id)
                        .orElseThrow(() -> new IllegalStateException(
                                String.format("gameState for gameId=%s does not contain host", gameState.id())));
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

    @SuppressWarnings("unchecked")
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        return actionCompiler.compile(action)
                             .flatMapTry(validAction -> {
                                 if (!allowedToRunActions(action.playerId(), gameState)) {
                                     return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
                                 }
                                 if (action.playerAction() == TurnAction.CANCEL_GAME) {
                                     return handleCancel(gameState, (Action<Void>) action);
                                 }
                                 if (action.playerAction() == TurnAction.ADMIT_PLAYER) {
                                     return handleAdmit(gameState, (Action<String>) action);
                                 }
                                 if (action.playerAction() == TurnAction.KICK_PLAYER) {
                                     return engineStateRegistry.get(EngineStateType.KICK)
                                                               .execute(gameState, action);
                                 }
                                 final var currentStateType = calculateCurrentState(gameState);
                                 final var engineState = engineStateRegistry.getOrDefault(currentStateType,
                                                                                          UNKNOWN_STATE);
                                 return engineState.execute(gameState, action);
                             });
    }

    private EngineStateType calculateCurrentState(GameState gameState) {
        if (gameState.currentTurn() == 0 && gameState.turns().isEmpty()) {
            return EngineStateType.INIT;
        } else if (gameState.currentTurn() < 0 || gameState.currentTurn() >= gameState.turns().size()) {
            return EngineStateType.INVALID_CURRENT_TURN;
        } else if (gameState.turns().get(gameState.currentTurn()).phase() == TurnPhase.SELECTION) {
            return EngineStateType.SELECTION;
        } else if (gameState.turns().get(gameState.currentTurn()).phase() == TurnPhase.REMOVAL) {
            return EngineStateType.REMOVAL;
        } else if (gameState.turns().get(gameState.currentTurn()).phase() == TurnPhase.GUESSING) {
            return EngineStateType.GUESS;
        } else if (gameState.turns().get(gameState.currentTurn()).phase() == TurnPhase.CONCLUSION) {
            return EngineStateType.CONCLUSION;
        } else {
            return EngineStateType.UNKNOWN;
        }
    }

    private boolean allowedToRunActions(String playerId, GameState gameState) {
        return Stream.concat(Stream.of(ROOT), gameState.players().stream()
                                                       .map(Player::id))
                     .collect(Collectors.toSet())
                     .contains(playerId);
    }

    private Try<GameState> handleCancel(GameState gameState, Action<Void> cancelAction) {
        if (hostOrRoot(cancelAction.playerId(), gameState)) {
            return Try.success(new GameState(
                    gameState.id(),
                    GameStatus.CANCELLED,
                    gameState.players(),
                    gameState.turns(),
                    gameState.wordsToGuess(),
                    gameState.currentTurn()
            ));
        }
        return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
    }

    private Try<GameState> handleAdmit(GameState gameState, Action<String> admitAction) {
        if (!hostOrRoot(admitAction.playerId(), gameState)) {
            return Try.failure(new IllegalActionException(ErrorCode.ILLEGAL_ACTION));
        }
        final var playerId = admitAction.payload();
        if (gameState.players().stream()
                     .anyMatch(p -> StringUtils.equals(p.id(), playerId))) {
            return Try.failure(new InvalidActionException(ErrorCode.ILLEGAL_ACTION));
        }
        return Try.success(new GameState(
                gameState.id(),
                gameState.status(),
                append(gameState.players().stream(), new Player(playerId, PlayerRole.PLAYER)).toList(),
                gameState.turns(),
                gameState.wordsToGuess(),
                gameState.currentTurn()
        ));
    }
}
