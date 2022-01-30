package com.github.fburato.justone.controllers;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fburato.justone.controllers.validation.EntityValidator;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.services.GameStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static com.github.fburato.justone.ArgumentMatchers.satisfies;
import static com.github.fburato.justone.RandomUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GameStateControllerTest {

    private final GameStateService gameStateService = mock(GameStateService.class);
    private final EntityValidator entityValidator = mock(EntityValidator.class);
    private final GameStateController gameStateController = new GameStateController(gameStateService, entityValidator);
    private final WebTestClient client = WebTestClient.bindToRouterFunction(gameStateController.routes())
            .build();
    private final String gameId = randomString();

    @Nested
    @DisplayName("on GET /games/{id}/state should")
    class GetGameStateTest {

        private final String uri = String.format("/games/%s/state", gameId);
        private final String notFoundMessage = String.format("State for game='%s' could not be found", gameId);

        @Test
        @DisplayName("resolve gameState from service")
        void resolveFromService() {
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.get()
                    .uri(uri)
                    .exchange();

            verify(gameStateService).getGameState(gameId);
        }

        @Test
        @DisplayName("return 404 with message if gameState is empty")
        void notFoundOnEmpty() {
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }

        @Test
        @DisplayName("return 200 with game state if game state is defined")
        void okOnFound() {
            final var gameState = randomGameState();
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.just(Optional.of(gameState)));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameState.class)
                    .isEqualTo(gameState);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpException() {
            final var exception = new RuntimeException(randomString());
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.error(exception));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on POST /games/{id}/state should")
    class CreateGameStateTest {

        private final String uri = String.format("/games/%s/state", gameId);
        private final GameStateService.CreateStateRequest request = new GameStateService.CreateStateRequest(
                randomString(), List.of(randomString(), randomString()), List.of(randomString(), randomString()));
        private final GameState gameState = randomGameState();
        private final GameStateService.CreateStateRequest validatedRequest = new GameStateService.CreateStateRequest(
                randomString(), List.of(randomString(), randomString()), List.of(randomString(), randomString()));

        @Test
        @DisplayName("validate body with entity validator")
        void validateRequestBody() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));

            request();

            verify(entityValidator).parseBodyAndValidate(satisfies(req ->
                    StepVerifier.create(req.bodyToMono(GameStateService.CreateStateRequest.class))
                            .expectNext(request)
                            .verifyComplete()), eq(GameStateService.CreateStateRequest.class));
        }

        private WebTestClient.ResponseSpec request() {
            return client.post()
                    .uri(uri)
                    .bodyValue(request)
                    .exchange();
        }

        @Test
        @DisplayName("resolve gameState from service passing validated request body")
        void resolveFromService() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.createGameState(anyString(), any())).thenReturn(Mono.just(gameState));

            request();

            verify(gameStateService).createGameState(gameId, validatedRequest);
        }

        @Test
        @DisplayName("return 200 with game state if game state is defined")
        void okOnFound() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.createGameState(anyString(), any())).thenReturn(Mono.just(gameState));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameState.class)
                    .isEqualTo(gameState);
        }

        @Test
        @DisplayName("bubble up exceptions generated during validation")
        void bubbleUpValidationExceptions() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(gameStateService);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpException() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.createGameState(anyString(), any()))
                    .thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on PUT /games/{id}/state should")
    class ExecuteActionTest {

        private final String uri = String.format("/games/%s/state", gameId);
        private final GameStateService.ActionRequest request = new GameStateService.ActionRequest(randomString(),
                randomEnum(
                        TurnAction.class),
                new TextNode(
                        randomString()));

        private final GameStateService.ActionRequest validatedRequest = new GameStateService.ActionRequest(randomString(),
                randomEnum(
                        TurnAction.class),
                new TextNode(
                        randomString()));
        private final GameState gameState = randomGameState();

        private final String notFoundMessage = String.format("State for game='%s' could not be found", gameId);

        @Test
        @DisplayName("validate entity with entity validator")
        void validateEntity() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.executeAction(anyString(), any())).thenReturn(Mono.just(Optional.empty()));

            request();

            verify(entityValidator).parseBodyAndValidate(satisfies(req ->
                    StepVerifier.create(req.bodyToMono(GameStateService.ActionRequest.class))
                            .expectNext(request)
                            .verifyComplete()), eq(GameStateService.ActionRequest.class));
        }

        private WebTestClient.ResponseSpec request() {
            return client.put()
                    .uri(uri)
                    .bodyValue(request)
                    .exchange();
        }

        @Test
        @DisplayName("execute action on service with validated action")
        void resolveFromService() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.executeAction(anyString(), any())).thenReturn(Mono.just(Optional.empty()));

            request();

            verify(gameStateService).executeAction(gameId, validatedRequest);
        }

        @Test
        @DisplayName("return 404 with message if gameState is empty")
        void notFoundOnEmpty() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.executeAction(anyString(), any())).thenReturn(Mono.just(Optional.empty()));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }


        @Test
        @DisplayName("return 200 with game state if game state is defined")
        void okOnFound() {
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.just(validatedRequest));
            when(gameStateService.executeAction(anyString(), any())).thenReturn(Mono.just(Optional.of(gameState)));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameState.class)
                    .isEqualTo(gameState);
        }

        @Test
        @DisplayName("bubble up exceptions generated from the validator")
        void bubbleUpValidationExceptions() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpException() {
            final var exception = new RuntimeException(randomString());
            when(gameStateService.executeAction(anyString(), any())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on DELETE /games/{id}/state should")
    class DeleteStateTest {
        private final String uri = String.format("/games/%s/state", gameId);
        private final GameState gameState = randomGameState();
        private final String notFoundMessage = String.format("State for game='%s' could not be found", gameId);


        @Test
        @DisplayName("resolve gameState from service")
        void resolveFromService() {
            when(gameStateService.getGameState(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.delete()
                    .uri(uri)
                    .exchange();

            verify(gameStateService).deleteGameState(gameId);
        }

        @Test
        @DisplayName("return 404 with message if gameState is empty")
        void notFoundOnEmpty() {
            when(gameStateService.deleteGameState(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.delete()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }

        @Test
        @DisplayName("return 200 with game state if game state is defined")
        void okOnFound() {
            when(gameStateService.deleteGameState(anyString())).thenReturn(Mono.just(Optional.of(gameState)));

            client.delete()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameState.class)
                    .isEqualTo(gameState);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpException() {
            final var exception = new RuntimeException(randomString());
            when(gameStateService.deleteGameState(anyString())).thenReturn(Mono.error(exception));

            client.delete()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}