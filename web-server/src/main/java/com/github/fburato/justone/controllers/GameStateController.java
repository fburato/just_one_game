package com.github.fburato.justone.controllers;

import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.services.GameStateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

public class GameStateController {

    private final GameStateService gameStateService;

    public GameStateController(GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    public RouterFunction<ServerResponse> routes() {
        return route()
                .nest(path("/games"), () -> route()
                        .add(getGameState())
                        .add(createGame())
                        .add(executeAction())
                        .add(deleteState())
                        .build())
                .build();
    }

    private RouterFunction<ServerResponse> getGameState() {
        return route(GET("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            return gameStateService.getGameState(id)
                                   .flatMap(gs -> toServerResponse(id, gs));
        });
    }

    private Mono<ServerResponse> toServerResponse(String id, Optional<GameState> maybeGameState) {
        return maybeGameState
                .map(gameState -> ok()
                        .body(BodyInserters.fromValue(gameState)))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND)
                        .body(BodyInserters.fromValue(
                                new ErrorDTO(String.format("State for game='%s' could not be found", id), List.of()))));
    }

    private RouterFunction<ServerResponse> createGame() {
        return route(POST("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            final var body = ensureDefined(req.bodyToMono(GameStateService.CreateStateRequest.class));
            return body.flatMap(requestBody -> gameStateService.createGameState(id, requestBody)
                                                               .flatMap(gameState -> ok()
                                                                       .body(BodyInserters.fromValue(gameState)))
            );
        });
    }

    private <T> Mono<T> ensureDefined(Mono<T> t) {
        return t.switchIfEmpty(nullBodyErrorMono())
                .flatMap(body -> {
                    if (body == null) {
                        return nullBodyErrorMono();
                    }
                    return Mono.just(body);
                });
    }

    private <T> Mono<T> nullBodyErrorMono() {
        return Mono.error(new IllegalArgumentException("body should be not null"));
    }

    private RouterFunction<ServerResponse> executeAction() {
        return route(PUT("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            final var action = ensureDefined(req.bodyToMono(GameStateService.ActionRequest.class));
            return action.flatMap(ar -> gameStateService.executeAction(id, ar)
                                                        .flatMap(gs -> toServerResponse(id, gs)));
        });
    }

    private RouterFunction<ServerResponse> deleteState() {
        return route(DELETE("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            return gameStateService.deleteGameState(id)
                                   .flatMap(gs -> toServerResponse(id, gs));
        });
    }
}
