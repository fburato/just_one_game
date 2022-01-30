package com.github.fburato.justone.wiring;

import com.github.fburato.justone.controllers.ErrorHandlerMiddleware;
import com.github.fburato.justone.controllers.GameConfigController;
import com.github.fburato.justone.controllers.GameStateController;
import com.github.fburato.justone.controllers.ValidationException;
import com.github.fburato.justone.controllers.validation.*;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.game.errors.EngineException;
import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.services.GameConfigService;
import com.github.fburato.justone.services.GameStateService;
import com.github.fburato.justone.services.errors.ConflictException;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ControllerWiring {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerWiring.class);

    static Optional<Tuple2<HttpStatus, ErrorDTO>> mapExceptions(Throwable t) {
        switch (t) {
            case EngineException engineException -> {
                LOG.warn("EngineException thrown with message={}", engineException.getMessage(), engineException);
                final var intErrorCodes = engineException.errorCodes().stream()
                        .map(ErrorCode::code)
                        .toList();
                final var all4xx = intErrorCodes.stream().allMatch(i -> i / 100_000 == 4);
                return Optional.of(Tuple.of(
                        all4xx ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR,
                        new ErrorDTO(t.getMessage(), intErrorCodes)
                ));
            }
            case ServerWebInputException serverWebInputException -> {
                LOG.warn("ServerWebInputException thrown with message={}", serverWebInputException.getMessage(),
                        serverWebInputException);
                return Optional.of(Tuple.of(HttpStatus.valueOf(serverWebInputException.getRawStatusCode()),
                        new ErrorDTO(serverWebInputException.getReason())));
            }
            case ValidationException validationException -> {
                LOG.info("User input failed to validate with messages: {}", validationException.validationErrors());
                return Optional.of(Tuple.of(HttpStatus.BAD_REQUEST, new ErrorDTO(validationException.validationErrors())));
            }
            case ConflictException conflictException -> {
                LOG.info("User attempted to create already existing entityType={} with entityId={}", conflictException.entityType(), conflictException.entityId());
                return Optional.of(Tuple.of(HttpStatus.CONFLICT, new ErrorDTO(String.format("conflict: entityType=%s with entityId=%s is already defined",
                        conflictException.entityType(), conflictException.entityId()))));
            }
            case EntityIdMismatchException entityIdMismatchException -> {
                LOG.info("User attempted to update entityId={} while accessing resourceId={}", entityIdMismatchException.entityId(), entityIdMismatchException.resourceId());
                return Optional.of(Tuple.of(
                        HttpStatus.BAD_REQUEST,
                        new ErrorDTO(String.format("the resourceId=%s of the request does not match entityId=%s",
                                entityIdMismatchException.resourceId(), entityIdMismatchException.entityId())
                        )));
            }
            case IllegalArgumentException iae -> {
                LOG.warn("IllegalArgumentException thrown with message={}", iae.getMessage(), iae);

                return Optional.of(Tuple.of(
                        HttpStatus.BAD_REQUEST,
                        new ErrorDTO(iae.getMessage())
                ));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Bean
    public EntityValidator entityValidator() {
        return new EntityValidator(List.of(
                new GameConfigValidator(),
                new CreateGameConfigRequestValidator(),
                new CreateStateRequestValidator(),
                new ActionRequestValidator()
        ));
    }

    @Bean
    public GameStateController gameStateController(GameStateService gameStateService, EntityValidator entityValidator) {
        return new GameStateController(gameStateService, entityValidator);
    }

    @Bean
    public GameConfigController gameConfigController(GameConfigService gameConfigService, EntityValidator entityValidator) {
        return new GameConfigController(gameConfigService, entityValidator);
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction(GameStateController gameStateController, GameConfigController gameConfigController) {
        final var errorMiddleware = new ErrorHandlerMiddleware(ControllerWiring::mapExceptions);
        final var compositeController = route()
                .add(gameStateController.routes())
                .add(gameConfigController.routes())
                .build();
        return errorMiddleware.decorate(compositeController);
    }

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }

}
