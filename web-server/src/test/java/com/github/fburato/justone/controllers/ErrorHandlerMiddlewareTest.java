package com.github.fburato.justone.controllers;

import com.github.fburato.justone.dtos.ErrorDTO;
import io.vavr.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

class ErrorHandlerMiddlewareTest {

    private final ErrorHandlerMiddleware testee = new ErrorHandlerMiddleware(t -> {
        if (t instanceof IllegalArgumentException) {
            return Optional.of(Tuple.of(HttpStatus.GATEWAY_TIMEOUT,
                                        new ErrorDTO(String.format("iae with message=%s", t.getMessage()), List.of())));
        }
        return Optional.empty();
    });
    private final RouterFunction<ServerResponse> testRoute = route(GET("/normal"), req -> ok()
            .bodyValue("foo"))
            .andRoute(GET("/illegalArgument"), req -> Mono.error(new IllegalArgumentException("foobar")))
            .andRoute(GET("/eagerFail"), req -> {
                throw new RuntimeException("foobaz");
            })
            .andRoute(GET("/controlledFail"), req -> Mono.error(new RuntimeException("barfoo")));
    WebTestClient webTestClient = WebTestClient.bindToRouterFunction(testee.decorate(testRoute))
                                               .build();

    @Test
    @DisplayName("should execute request normally if no error is returned")
    void routeNormally() {
        webTestClient.get()
                     .uri("/normal")
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(String.class)
                     .isEqualTo("foo");
    }

    @Test
    @DisplayName("should convert exception handled by the partial function into the expected status code and error body")
    void handledException() {
        webTestClient.get()
                     .uri("/illegalArgument")
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.GATEWAY_TIMEOUT)
                     .expectBody(ErrorDTO.class)
                     .isEqualTo(new ErrorDTO("iae with message=foobar", List.of()));
    }

    @Test
    @DisplayName("should convert eagerly thrown exception into 500 with generic error body")
    void eagerHandling() {
        webTestClient.get()
                     .uri("/eagerFail")
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                     .expectBody(ErrorDTO.class)
                     .isEqualTo(new ErrorDTO("Unhandled exception with message='foobaz'", List.of()));
    }

    @Test
    @DisplayName("should convert reactively thrown exception into 500 with generic error body")
    void controlledError() {
        webTestClient.get()
                     .uri("/controlledFail")
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                     .expectBody(ErrorDTO.class)
                     .isEqualTo(new ErrorDTO("Unhandled exception with message='barfoo'", List.of()));
    }

    @Test
    @DisplayName("should return 404 with message if route is not registered")
    void notFound() {
        webTestClient.patch()
                     .uri("/notRouted")
                     .exchange()
                     .expectStatus()
                     .isEqualTo(HttpStatus.NOT_FOUND)
                     .expectBody(ErrorDTO.class)
                     .isEqualTo(new ErrorDTO("'PATCH /notRouted' is not handled by this server", List.of()));
    }
}