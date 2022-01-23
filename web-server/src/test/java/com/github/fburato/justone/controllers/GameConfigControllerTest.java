package com.github.fburato.justone.controllers;

import com.github.fburato.justone.controllers.validation.EntityValidator;
import com.github.fburato.justone.dtos.ErrorDTO;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.services.GameConfigService;
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
import static com.github.fburato.justone.RandomUtils.randomGameConfig;
import static com.github.fburato.justone.RandomUtils.randomString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GameConfigControllerTest {

    private final GameConfigService gameConfigService = mock(GameConfigService.class);
    private final EntityValidator entityValidator = mock(EntityValidator.class);
    private final GameConfigController gameConfigController = new GameConfigController(gameConfigService, entityValidator);
    private final WebTestClient client = WebTestClient.bindToRouterFunction(gameConfigController.routes())
            .build();
    private final String gameId = randomString();

    @Nested
    @DisplayName("on GET /games/{id}/config should")
    class GetGameConfigTests {

        private final String uri = String.format("/games/%s/config", gameId);
        private final String notFoundMessage = String.format("Config for game='%s' could not be found", gameId);

        @Test
        @DisplayName("resolve gameConfig from service")
        void resolveFromService() {
            when(gameConfigService.getGameConfig(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.get()
                    .uri(uri)
                    .exchange();

            verify(gameConfigService).getGameConfig(gameId);
        }

        @Test
        @DisplayName("return 404 with message if gameState is empty")
        void notFoundOnEmpty() {
            when(gameConfigService.getGameConfig(anyString())).thenReturn(Mono.just(Optional.empty()));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }

        @Test
        @DisplayName("return 200 with game state if game is defined")
        void okOnFound() {
            final var gameConfig = randomGameConfig();
            when(gameConfigService.getGameConfig(anyString())).thenReturn(Mono.just(Optional.of(gameConfig)));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameConfig.class)
                    .isEqualTo(gameConfig);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpExceptions() {
            final var exception = new RuntimeException(randomString());
            when(gameConfigService.getGameConfig(anyString())).thenReturn(Mono.error(exception));

            client.get()
                    .uri(uri)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on POST /games/{id}/config should")
    class CreateGameConfigTest {
        private final String uri = String.format("/games/%s/config", gameId);
        private final GameConfig inputConfig = randomGameConfig();
        private final GameConfigController.CreateGameConfigRequest requestBody = new GameConfigController.CreateGameConfigRequest(
                inputConfig.host(), inputConfig.languageId(), inputConfig.wordPackNames()
        );
        private final GameConfigController.CreateGameConfigRequest validatedRequest =
                new GameConfigController.CreateGameConfigRequest(randomString(), randomString(), List.of(randomString(), randomString()));
        private final GameConfig outputConfig = randomGameConfig();

        private WebTestClient.ResponseSpec request() {
            return client.post()
                    .uri(uri)
                    .bodyValue(requestBody)
                    .exchange();
        }


        @Test
        @DisplayName("validate body with entity validator")
        void validateBody() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedRequest));
            when(gameConfigService.createGameConfig(any())).thenReturn(Mono.just(outputConfig));

            request();

            verify(entityValidator).parseBodyAndValidate(satisfies(req ->
                    StepVerifier.create(req.bodyToMono(GameConfigController.CreateGameConfigRequest.class))
                            .expectNext(requestBody)
                            .verifyComplete()), eq(GameConfigController.CreateGameConfigRequest.class));
        }

        @Test
        @DisplayName("fail with 400 if gameId is blank")
        void blankGameId400() {
            client.post()
                    .uri("/games/    /config")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.BAD_REQUEST)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO("gameid='    ' should not be blank"));
        }

        @Test
        @DisplayName("create validated config in service")
        void createConfigInService() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedRequest));
            when(gameConfigService.createGameConfig(any())).thenReturn(Mono.just(outputConfig));

            request();

            verify(gameConfigService).createGameConfig(new GameConfig(gameId, validatedRequest.host(), validatedRequest.languageId(), validatedRequest.wordPackNames()));
        }

        @Test
        @DisplayName("return the output config as response")
        void returnServiceOutput() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedRequest));
            when(gameConfigService.createGameConfig(any())).thenReturn(Mono.just(outputConfig));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameConfig.class)
                    .isEqualTo(outputConfig);
        }

        @Test
        @DisplayName("fail if entity validation fails")
        void failOnEntityValidation() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(gameConfigService);
        }

        @Test
        @DisplayName("fail if creation on service fails")
        void failOnServiceFailure() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedRequest));
            when(gameConfigService.createGameConfig(any())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on PUT /games/{id}/state should")
    class UpdateStateTest {
        private final String uri = String.format("/games/%s/config", gameId);
        private final GameConfig inputConfig = randomGameConfig();
        private final GameConfig validatedConfig = randomGameConfig();
        private final GameConfig outputConfig = randomGameConfig();
        private final String notFoundMessage = String.format("Config for game='%s' could not be found", gameId);

        private WebTestClient.ResponseSpec request() {
            return client.put()
                    .uri(uri)
                    .bodyValue(inputConfig)
                    .exchange();
        }

        @Test
        @DisplayName("validate body with entity validator")
        void validateBody() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedConfig));
            when(gameConfigService.updateGameConfig(any(), any())).thenReturn(Mono.just(Optional.of(outputConfig)));

            request();

            verify(entityValidator).parseBodyAndValidate(any(), eq(GameConfig.class));
        }

        @Test
        @DisplayName("update validated config in service")
        void createConfigInService() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedConfig));
            when(gameConfigService.updateGameConfig(any(), any())).thenReturn(Mono.just(Optional.of(outputConfig)));

            request();

            verify(gameConfigService).updateGameConfig(gameId, validatedConfig);
        }

        @Test
        @DisplayName("return the output config as response")
        void returnServiceOutput() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedConfig));
            when(gameConfigService.updateGameConfig(any(), any())).thenReturn(Mono.just(Optional.of(outputConfig)));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameConfig.class)
                    .isEqualTo(outputConfig);
        }

        @Test
        @DisplayName("return a 404 if service returns empty result")
        void notFoundOnEntityEmpty() {
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedConfig));
            when(gameConfigService.updateGameConfig(any(), any())).thenReturn(Mono.just(Optional.empty()));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }

        @Test
        @DisplayName("fail if entity validation fails")
        void failOnEntityValidation() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(gameConfigService);
        }

        @Test
        @DisplayName("fail if creation on service fails")
        void failOnServiceFailure() {
            final var exception = new RuntimeException(randomString());
            when(entityValidator.parseBodyAndValidate(any(), any()))
                    .thenReturn(Mono.just(validatedConfig));
            when(gameConfigService.updateGameConfig(any(), any())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("on DELETE /games/{id}/config should")
    class DeleteConfigTest {
        private final String uri = String.format("/games/%s/config", gameId);
        private final String notFoundMessage = String.format("Config for game='%s' could not be found", gameId);

        @Test
        @DisplayName("delete gameConfig from service")
        void resolveFromService() {
            when(gameConfigService.deleteGameConfig(anyString())).thenReturn(Mono.just(Optional.empty()));

            request();

            verify(gameConfigService).deleteGameConfig(gameId);
        }

        private WebTestClient.ResponseSpec request() {
            return client.delete()
                    .uri(uri)
                    .exchange();
        }

        @Test
        @DisplayName("return 404 with message if gameState is empty")
        void notFoundOnEmpty() {
            when(gameConfigService.deleteGameConfig(anyString())).thenReturn(Mono.just(Optional.empty()));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ErrorDTO.class)
                    .isEqualTo(new ErrorDTO(notFoundMessage));
        }

        @Test
        @DisplayName("return 200 with game state if game is defined")
        void okOnFound() {
            final var gameConfig = randomGameConfig();
            when(gameConfigService.deleteGameConfig(anyString())).thenReturn(Mono.just(Optional.of(gameConfig)));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.OK)
                    .expectBody(GameConfig.class)
                    .isEqualTo(gameConfig);
        }

        @Test
        @DisplayName("bubble up exceptions if they are raised from the service")
        void bubbleUpExceptions() {
            final var exception = new RuntimeException(randomString());
            when(gameConfigService.deleteGameConfig(anyString())).thenReturn(Mono.error(exception));

            request()
                    .expectStatus()
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}