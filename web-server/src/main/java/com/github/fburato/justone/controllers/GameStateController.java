package com.github.fburato.justone.controllers;

import com.github.fburato.justone.controllers.validation.EntityValidator;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.services.GameStateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

public class GameStateController {

    private final GameStateService gameStateService;
    private final EntityValidator entityValidator;

    public GameStateController(GameStateService gameStateService, EntityValidator entityValidator) {
        this.gameStateService = gameStateService;
        this.entityValidator = entityValidator;
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
                                new ErrorDTO(String.format("State for game='%s' could not be found", id)))));
    }

    private RouterFunction<ServerResponse> createGame() {
        return route(POST("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            final var body = entityValidator.parseBodyAndValidate(req, GameStateService.CreateStateRequest.class);
            return body.flatMap(requestBody -> gameStateService.createGameState(id, requestBody)
                    .flatMap(gameState -> ok()
                            .body(BodyInserters.fromValue(gameState)))
            );
        });
    }

    private RouterFunction<ServerResponse> executeAction() {
        return route(PUT("/{id}/state"), req -> {
            final var id = req.pathVariable("id");
            final var action = entityValidator.parseBodyAndValidate(req, GameStateService.ActionRequest.class);
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
