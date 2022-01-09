package com.github.fburato.justone.controllers;

import com.github.fburato.justone.dtos.ErrorDTO;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

public class ErrorHandlerMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlerMiddleware.class);

    private final Function<Throwable, Optional<Tuple2<HttpStatus, ErrorDTO>>> errorMapper;

    public ErrorHandlerMiddleware(Function<Throwable, Optional<Tuple2<HttpStatus, ErrorDTO>>> errorMapper) {
        this.errorMapper = errorMapper;
    }

    public RouterFunction<ServerResponse> decorate(RouterFunction<ServerResponse> routerFunction) {
        final var unknown = route(req -> true,
                req -> status(HttpStatus.NOT_FOUND)
                        .body(BodyInserters.fromValue(new ErrorDTO(
                                String.format("'%s %s' is not handled by this server", req.method(),
                                        req.path())))));
        return req -> routerFunction.and(unknown).route(req)
                .map(handlerFunction -> req1 -> {
                    Mono<ServerResponse> response;
                    try {
                        response = handlerFunction.handle(req1);
                    } catch (RuntimeException e) {
                        response = Mono.error(e);
                    }
                    return response.onErrorResume(throwable -> {

                        final var error = errorMapper.apply(throwable)
                                .orElseGet(() -> {
                                    LOG.error(
                                            "Unhandled exception thrown with message={}",
                                            throwable.getMessage(), throwable);
                                    return Tuple.of(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            new ErrorDTO(String.format(
                                                    "Unhandled exception with message='%s'",
                                                    throwable.getMessage())));
                                });
                        return status(error._1)
                                .body(BodyInserters.fromValue(error._2));
                    });
                });
    }
}
