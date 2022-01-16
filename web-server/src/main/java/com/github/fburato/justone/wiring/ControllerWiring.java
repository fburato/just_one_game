package com.github.fburato.justone.wiring;

import com.github.fburato.justone.controllers.ErrorHandlerMiddleware;
import com.github.fburato.justone.controllers.GameStateController;
import com.github.fburato.justone.controllers.ValidationException;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.game.errors.EngineException;
import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.services.GameStateService;
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

import java.util.Optional;

@Configuration
public class ControllerWiring {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerWiring.class);

    static Optional<Tuple2<HttpStatus, ErrorDTO>> mapExceptions(Throwable t) {
        if (t instanceof final EngineException engineException) {
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
        if (t instanceof final ServerWebInputException serverWebInputException) {
            LOG.warn("ServerWebInputException thrown with message={}", serverWebInputException.getMessage(),
                    serverWebInputException);
            return Optional.of(Tuple.of(HttpStatus.valueOf(serverWebInputException.getRawStatusCode()),
                    new ErrorDTO(serverWebInputException.getReason())));
        }
        if (t instanceof final ValidationException validationException) {
            LOG.info("User input failed to validate with messages: {}", validationException.validationErrors());
            return Optional.of(Tuple.of(HttpStatus.BAD_REQUEST, new ErrorDTO(validationException.validationErrors())));
        }
        if (t instanceof final IllegalArgumentException iae) {
            LOG.warn("IllegalArgumentException thrown with message={}", iae.getMessage(), iae);

            return Optional.of(Tuple.of(
                    HttpStatus.BAD_REQUEST,
                    new ErrorDTO(iae.getMessage())
            ));
        }
        return Optional.empty();
    }

    @Bean
    public GameStateController gameStateController(GameStateService gameStateService) {
        return new GameStateController(gameStateService);
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction(GameStateController gameStateController) {
        final var errorMiddleware = new ErrorHandlerMiddleware(ControllerWiring::mapExceptions);
        final var compositeController = gameStateController.routes();
        return errorMiddleware.decorate(compositeController);
    }

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }

}
