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
    SELECTION,
    REMOVAL,
    GUESS,
    KICK,
    INVALID_CURRENT_TURN,
    UNKNOWN
}

interface EngineState {
    Try<GameState> execute(GameState gameState, Action<?> action);
}

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
            EngineStateType.GUESS, new GuessingState()
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

        final Set<String> strippedCaseInsensitiveHints = hintsOfProviders.values().stream()
                                                                         .map(SelectionState::normalise)
                                                                         .collect(Collectors.toSet());
        final List<String> toExclude = hintsOfProviders.values().stream()
                                                       .filter(hint -> strippedCaseInsensitiveHints.contains(
                                                               normalise(hint))).toList();
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
        final List<PlayerWord> hintsToRemove;
        if (turn.providedHints().stream()
                .map(PlayerWord::word)
                .collect(Collectors.toSet()).contains(toRemove) &&
                !turn.hintsToRemove().stream()
                     .map(PlayerWord::word)
                     .collect(Collectors.toSet()).contains(toRemove) &&
                !turn.hintsToFilter().contains(toRemove)) {
            hintsToRemove = List.of(new PlayerWord(remover, toRemove));
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