package com.github.fburato.justone.controllers;

import com.github.fburato.justone.controllers.validation.EntityValidator;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.services.GameConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

public class GameConfigController {

    private static final String RESOURCE_BY_ID_FRAGMENT = "/{id}/config";
    private static final String ID_PARAMETER = "id";
    private final GameConfigService gameConfigService;
    private final EntityValidator entityValidator;

    public GameConfigController(GameConfigService gameConfigService, EntityValidator entityValidator) {
        this.gameConfigService = gameConfigService;
        this.entityValidator = entityValidator;
    }

    public RouterFunction<ServerResponse> routes() {

        return route()
                .nest(path("/games"), () -> route()
                        .add(getGameConfig())
                        .add(createGameConfig())
                        .add(updateGameConfig())
                        .add(deleteGameConfig())
                        .build())
                .build();
    }

    private RouterFunction<ServerResponse> getGameConfig() {
        return route(GET(RESOURCE_BY_ID_FRAGMENT), req -> {
            final var id = req.pathVariable(ID_PARAMETER);
            return gameConfigService.getGameConfig(id)
                    .flatMap(gc -> toServerResponse(id, gc));
        });
    }

    private Mono<ServerResponse> toServerResponse(String id, Optional<GameConfig> maybeGameConfig) {
        return maybeGameConfig
                .map(gameConfig -> ok()
                        .body(BodyInserters.fromValue(gameConfig)))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND)
                        .body(BodyInserters.fromValue(
                                new ErrorDTO(String.format("Config for game='%s' could not be found", id))
                        )));
    }

    private RouterFunction<ServerResponse> createGameConfig() {
        return route(POST(RESOURCE_BY_ID_FRAGMENT), req -> {
            final var id = req.pathVariable(ID_PARAMETER);
            if (StringUtils.isBlank(id)) {
                return badRequest()
                        .bodyValue(new ErrorDTO(String.format("gameid='%s' should not be blank", id)));
            }
            return entityValidator.parseBodyAndValidate(req, CreateGameConfigRequest.class)
                    .map(request -> new GameConfig(id, request.host(), request.languageId(), request.wordPackNames()))
                    .flatMap(gameConfig -> gameConfigService.createGameConfig(gameConfig)
                            .flatMap(outputConfig -> ok().body(BodyInserters.fromValue(outputConfig))));
        });
    }

    private RouterFunction<ServerResponse> updateGameConfig() {
        return route(PUT(RESOURCE_BY_ID_FRAGMENT), req -> {
            final var id = req.pathVariable(ID_PARAMETER);
            return entityValidator.parseBodyAndValidate(req, GameConfig.class)
                    .flatMap(gameConfig ->
                            gameConfigService.updateGameConfig(id, gameConfig)
                                    .flatMap(maybeGameConfig -> toServerResponse(id, maybeGameConfig)));
        });
    }

    private RouterFunction<ServerResponse> deleteGameConfig() {
        return route(DELETE(RESOURCE_BY_ID_FRAGMENT), req -> {
            final var id = req.pathVariable(ID_PARAMETER);
            return gameConfigService.deleteGameConfig(id)
                    .flatMap(maybeGameConfig -> toServerResponse(id, maybeGameConfig));
        });
    }


    public record CreateGameConfigRequest(String host, String languageId, List<String> wordPackNames) {
    }
}
