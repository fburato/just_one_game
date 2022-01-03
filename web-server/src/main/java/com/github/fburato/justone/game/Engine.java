package com.github.fburato.justone.game;

import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.game.errors.InvalidActionException;
import com.github.fburato.justone.game.errors.InvalidStateException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.GameStatus;
import com.github.fburato.justone.model.Player;
import com.github.fburato.justone.model.PlayerRole;
import com.github.fburato.justone.model.PlayerWord;
import com.github.fburato.justone.model.Turn;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.model.TurnPhase;
import com.github.fburato.justone.model.TurnPlayer;
import com.github.fburato.justone.model.TurnRole;
import io.vavr.control.Try;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.fburato.justone.game.Engine.hostOrRoot;
import static com.github.fburato.justone.utils.StreamUtils.append;

enum EngineStateType {
    INIT,
    KICK,
    UNKNOWN
}

interface EngineState {
    Try<GameState> execute(GameState gameState, Action<?> action);
}

public class Engine {

    public static final String ROOT = "root";
    private static final EngineState UNKOWN_STATE = new UnknownState();
    private final ActionCompiler actionCompiler;
    private final Map<EngineStateType, EngineState> engineStateRegistry = Map.of(
            EngineStateType.INIT, new InitState(),
            EngineStateType.KICK, new KickState()
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
                                                                                          UNKOWN_STATE);
                                 return engineState.execute(gameState, action);
                             });
    }

    private EngineStateType calculateCurrentState(GameState gameState) {
        if (gameState.currentTurn() == 0 && gameState.turns().isEmpty()) {
            return EngineStateType.INIT;
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

class UnknownState implements EngineState {

    @Override
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        return Try.failure(new InvalidStateException(ErrorCode.UNRECOGNISED_STATE));
    }
}

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

class SelectionState implements EngineState {

    @Override
    public Try<GameState> execute(GameState gameState, Action<?> action) {
        return null;
    }
}