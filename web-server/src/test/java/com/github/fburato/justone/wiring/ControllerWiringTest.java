package com.github.fburato.justone.wiring;

import com.github.fburato.justone.controllers.GameConfigController;
import com.github.fburato.justone.controllers.ValidationException;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.IllegalActionException;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.services.GameConfigService;
import com.github.fburato.justone.services.GameStateService;
import com.github.fburato.justone.services.errors.ConflictException;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration
class ControllerWiringTest {

    @MockBean
    private GameStateService gameStateService;

    @MockBean
    private GameConfigService gameConfigService;

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

    @Test
    @DisplayName("should wire GameStateController")
    void gameStateWired() {
        webTestClient
                .get()
                .uri("/games/gameId1/state")
                .exchange();

        verify(gameStateService).getGameState("gameId1");
    }

    @Test
    @DisplayName("should wire GameConfigController")
    void gameConfigWired() {

        webTestClient
                .get()
                .uri("/games/gameId2/config")
                .exchange();

        verify(gameConfigService).getGameConfig("gameId2");
    }

    @Test
    @DisplayName("should wire CreateGameConfigRequest validator")
    void createGameConfigRequestValidator() {
        webTestClient
                .post()
                .uri("/games/gameId1/config")
                .bodyValue(new GameConfigController.CreateGameConfigRequest("", "", List.of()))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorDTO.class)
                .isEqualTo(new ErrorDTO(List.of("host='' is blank while it should be defined",
                        "languageId='' is blank while it should be defined",
                        "wordPackNames=[] is an empty-list while it should be non-empty")));
    }

    @Test
    @DisplayName("should wire GameConfig validator")
    void gameConfigValidator() {
        webTestClient
                .put()
                .uri("/games/gameId1/config")
                .bodyValue(new GameConfig("", "", "", List.of()))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorDTO.class)
                .isEqualTo(new ErrorDTO(List.of("gameId='' is blank while it should be defined",
                        "host='' is blank while it should be defined",
                        "languageId='' is blank while it should be defined",
                        "wordPackNames=[] is an empty-list while it should be non-empty")));
    }

    @Test
    @DisplayName("should wire CreateStateRequestValidator")
    void createStateRequestValidator() {
        webTestClient
                .post()
                .uri("/games/gameId1/state")
                .bodyValue(new GameStateService.CreateStateRequest(null, List.of(), List.of()))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorDTO.class)
                .isEqualTo(new ErrorDTO(List.of(
                        "host='null' is blank while it should be defined",
                        "players=[] is an empty-list while it should be non-empty",
                        "wordsToGuess=[] is an empty-list while it should be non-empty"
                )));
    }

    @Test
    @DisplayName("should wire ActionRequest validator")
    void actionRequestValidator() {
        webTestClient
                .put()
                .uri("/games/gameId1/state")
                .bodyValue(new GameStateService.ActionRequest(null, TurnAction.ADMIT_PLAYER, null))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorDTO.class)
                .isEqualTo(new ErrorDTO(List.of(
                        "playerId='null' is blank while it should be defined"
                )));
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

        @Test
        @DisplayName("return a 400 error for validation exceptions")
        void validation400() {
            final var errorMessage = randomString();
            when(gameStateService.deleteGameState(anyString()))
                    .thenReturn(Mono.error(new ValidationException(List.of(errorMessage))));

            webTestClient
                    .delete()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(List.of(errorMessage)));
        }

        @Test
        @DisplayName("return a 409 for a conflict exception")
        void conflict409() {
            final var exception = new ConflictException(randomString(), randomString());
            when(gameStateService.deleteGameState(anyString()))
                    .thenReturn(Mono.error(exception));

            webTestClient
                    .delete()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(String.format("conflict: entityType=%s with entityId=%s is already defined",
                            exception.entityType(), exception.entityId())));
        }

        @Test
        @DisplayName("return a 400 for a EntityMismatchException")
        void entityMismatch400() {
            final var exception = new EntityIdMismatchException(randomString(), randomString());
            when(gameStateService.deleteGameState(anyString()))
                    .thenReturn(Mono.error(exception));

            webTestClient
                    .delete()
                    .uri("/games/gameId1/state")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(String.format("the resourceId=%s of the request does not match entityId=%s",
                            exception.resourceId(), exception.entityId())));
        }
    }
}