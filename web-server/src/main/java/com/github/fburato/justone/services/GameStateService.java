package com.github.fburato.justone.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fburato.justone.game.engine.Engine;
import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.TurnAction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static com.github.fburato.justone.utils.TryUtils.toMono;

public class GameStateService {

    private final CrudRepository<String, GameState> gameStateRepository;
    private final Engine engine;

    public GameStateService(Engine engine, CrudRepository<String, GameState> gameStateRepository) {
        this.gameStateRepository = gameStateRepository;
        this.engine = engine;
    }

    public Mono<Optional<GameState>> getGameState(String gameId) {
        return Mono.just(gameStateRepository.findById(gameId));
    }

    public Mono<GameState> createGameState(String id, CreateStateRequest createStateRequest) {
        final var tryState = engine.init(id, createStateRequest.host(), createStateRequest.players(),
                                         createStateRequest.wordsToGuess());
        return toMono(tryState.onSuccess(gameStateRepository::save));
    }

    public Mono<Optional<GameState>> executeAction(String gameId, ActionRequest actionRequest) {
        final var maybeGameState = gameStateRepository.findById(gameId);
        if (maybeGameState.isEmpty()) {
            return Mono.just(maybeGameState);
        }
        final var gameState = maybeGameState.get();
        return compileActionRequest(actionRequest)
                .flatMap(compiledAction -> toMono(engine.execute(gameState, compiledAction)))
                .map(gs -> {
                    gameStateRepository.save(gs);
                    return Optional.of(gs);
                });
    }

    private Mono<Action<?>> compileActionRequest(ActionRequest actionRequest) {
        switch (actionRequest.turnAction()) {
            case PROCEED, CANCEL_GAME, CANCEL_PROVIDED_HINT -> {
                return Mono.just(new Action<>(actionRequest.playerId(), actionRequest.turnAction(), Void.class, null));
            }
            default -> {
                if (!Optional.ofNullable(actionRequest.payload())
                             .map(JsonNode::isTextual)
                             .orElse(false)) {
                    return Mono.error(
                            new IllegalArgumentException(String.format("expected a string payload got '%s' instead",
                                                                       actionRequest.payload())));
                }
                return Mono.just(new Action<>(actionRequest.playerId(), actionRequest.turnAction(), String.class,
                                              actionRequest.payload().asText()));
            }
        }
    }

    public Mono<Optional<GameState>> deleteGameState(String gameId) {
        return Mono.just(gameStateRepository.findById(gameId)
                                            .map(gs -> {
                                                gameStateRepository.delete(gameId);
                                                return gs;
                                            }));
    }

    public Flux<GameState> getAllGameStates() {

        return Flux.fromStream(gameStateRepository.getAll());
    }

    public Flux<String> getAllGameStatesId() {

        return getAllGameStates()
                .map(GameState::id);
    }

    public record CreateStateRequest(String host, List<String> players, List<String> wordsToGuess) {
    }

    public record ActionRequest(String playerId, TurnAction turnAction, JsonNode payload) {
    }
}
