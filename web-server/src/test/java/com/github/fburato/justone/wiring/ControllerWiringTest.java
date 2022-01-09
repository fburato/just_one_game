package com.github.fburato.justone.wiring;

import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.services.GameStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
class ControllerWiringTest {

    @MockBean
    private GameStateService gameStateService;
    @Autowired
    private RouterFunction<ServerResponse> routerFunction;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction)
                .build();
    }

    @Test
    @DisplayName("should wire with only GameStateServiceMock")
    void wireWithServiceMock() {
        assertThat(routerFunction).isNotNull();
    }

    @Configuration
    @Import(ControllerWiring.class)
    static class TestConfig {

    }

    @Nested
    @DisplayName("router decorated with error middleware should")
    class ErrorHandlingTest {
        @Test
        @DisplayName("return standard 404 not found error on un-handled request")
        void decorateWithErrorHandlerMiddleware() {
            webTestClient
                    .patch()
                    .uri("/foobar")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("'PATCH /foobar' is not handled by this server"));

        }

        @Test
        @DisplayName("return a 400 with standard message if generic server input exception is thrown")
        void standardInput() {
            webTestClient
                    .post()
                    .uri("/games/gameId1/state")
                    .bodyValue(42)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("Failed to read HTTP message"));

        }

        @Test
        @DisplayName("return an undecorated specific 404 message if controller returns it")
        void standardErrorResponse() {
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.just(Optional.empty()));

            webTestClient
                    .get()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("State for game='gameId1' could not be found"));

        }

        @Test
        @DisplayName("return a 400 response for engine exception only of type 4xx_xxx")
        void badRequestOnOnly4xxxxxErrorCodes() {
            when(gameStateService.executeAction(anyString(), any()))
                    .thenReturn(Mono.error(new IllegalActionException(ErrorCode.ILLEGAL_ACTION)));

            webTestClient
                    .put()
                    .uri("/games/gameId1/state")
                    .bodyValue(new GameStateService.ActionRequest(randomString(), TurnAction.PROCEED, null))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("ILLEGAL_ACTION", List.of(ErrorCode.ILLEGAL_ACTION.code())));
        }

        @Test
        @DisplayName("return a 500 response for engine exception with at least a non 4xx_xxx")
        void internalServerErrorNotOnOnly4xxxxxErrorCodes() {
            when(gameStateService.executeAction(anyString(), any()))
                    .thenReturn(Mono.error(new IllegalActionException(ErrorCode.ILLEGAL_ACTION, ErrorCode.UNKNOWN)));

            webTestClient
                    .put()
                    .uri("/games/gameId1/state")
                    .bodyValue(new GameStateService.ActionRequest(randomString(), TurnAction.PROCEED, null))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("ILLEGAL_ACTION, UNKNOWN",
                            List.of(ErrorCode.ILLEGAL_ACTION.code(), ErrorCode.UNKNOWN.code())));
        }

        @Test
        @DisplayName("return a 400 when an IllegalArgumentException is thrown")
        void badRequestOnIllegalArgumentException() {
            webTestClient
                    .put()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("body should be not null"));

        }

        @Test
        @DisplayName("return a generic 500 for exceptions not explicitly matched")
        void generic500() {
            final var errorMessage = randomString();
            when(gameStateService.deleteGameState(anyString()))
                    .thenReturn(Mono.error(new RuntimeException(errorMessage)));

            webTestClient
                    .delete()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(String.format("Unhandled exception with message='%s'", errorMessage)));
        }
    }
}