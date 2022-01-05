package com.github.fburato.justone.services;


import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fburato.justone.game.engine.Engine;
import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.io.InMemoryCrudRepository;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.model.TurnAction;
import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.fburato.justone.RandomUtils.randomGameState;
import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameStateServiceTest {

    private final CrudRepository<String, GameState> gameStateRepository = new InMemoryCrudRepository<>(GameState::id);
    private final Engine engine = mock(Engine.class);
    private final GameStateService testee = new GameStateService(engine, gameStateRepository);

    private final GameState gameState1 = randomGameState();
    private final GameState gameState2 = randomGameState();
    private final String id = randomString();
    private final Consumer<GameState> anyGameState = gs -> {
    };

    private static <T> void anyNext(T t) {
    }

    @Nested
    @DisplayName("on getGameState")
    class GetGameStateTest {

        @Test
        @DisplayName("should return empty if repository returns empty")
        void emptyOnEmpty() {
            final var result = testee.getGameState(gameState1.id());

            StepVerifier.create(result)
                        .expectNext(Optional.empty())
                        .verifyComplete();
        }

        @Test
        @DisplayName("should return game state if present in repository")
        void gameStateOnDefined() {
            gameStateRepository.save(gameState1);

            StepVerifier.create(testee.getGameState(gameState1.id()))
                        .expectNext(Optional.of(gameState1))
                        .verifyComplete();
        }

        @Test
        @DisplayName("should return only the game by id if more are present")
        void onlySelected() {
            gameStateRepository.saveAll(List.of(gameState1, gameState2));

            StepVerifier.create(testee.getGameState(gameState1.id()))
                        .expectNext(Optional.of(gameState1))
                        .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on getAllGameStates")
    class GetAllGameStatesTest {
        @Test
        @DisplayName("should return empty if no game state is in repository")
        void emptyOnNothing() {
            StepVerifier.create(testee.getAllGameStates())
                        .verifyComplete();
        }

        @Test
        @DisplayName("should return all saved game states")
        void allGameStates() {
            gameStateRepository.saveAll(List.of(gameState1, gameState2));

            StepVerifier.create(testee.getAllGameStates().buffer(2))
                        .assertNext(states -> assertThat(states).containsExactlyInAnyOrder(gameState1, gameState2))
                        .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on getAllGameStatesId")
    class GetAllGameStatesIdTest {
        @Test
        @DisplayName("should return empty if no game state is in repository")
        void emptyOnNothing() {
            StepVerifier.create(testee.getAllGameStatesId())
                        .verifyComplete();
        }

        @Test
        @DisplayName("should return all saved game state ids")
        void allGameStates() {
            gameStateRepository.saveAll(List.of(gameState1, gameState2));

            StepVerifier.create(testee.getAllGameStatesId().buffer(2))
                        .assertNext(states -> assertThat(states).containsExactlyInAnyOrder(gameState1.id(),
                                                                                           gameState2.id()))
                        .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on createGameState")
    class CreateGameStateTest {

        private final GameStateService.CreateStateRequest request = new GameStateService.CreateStateRequest(
                randomString(),
                List.of(randomString(), randomString()),
                List.of(randomString(), randomString()));

        @Test
        @DisplayName("should generate game state from engine")
        void initFromEngine() {
            when(engine.init(anyString(), anyString(), any(), any()))
                    .thenReturn(Try.success(gameState1));

            StepVerifier.create(testee.createGameState(id, request))
                        .assertNext(GameStateServiceTest::anyNext)
                        .verifyComplete();

            verify(engine).init(id, request.host(), request.players(), request.wordsToGuess());
        }

        @Test
        @DisplayName("should return game generated by engine")
        void returnGameFromEngine() {

            when(engine.init(anyString(), anyString(), any(), any()))
                    .thenReturn(Try.success(gameState1));

            StepVerifier.create(testee.createGameState(id, request))
                        .expectNext(gameState1)
                        .verifyComplete();
        }

        @Test
        @DisplayName("should save game generated by engine in repository")
        void saveGameInRepository() {
            when(engine.init(anyString(), anyString(), any(), any()))
                    .thenReturn(Try.success(gameState1));

            StepVerifier.create(testee.createGameState(id, request))
                        .assertNext(anyGameState)
                        .verifyComplete();
            assertThat(gameStateRepository.getAll())
                    .contains(gameState1);
        }

        @Test
        @DisplayName("should return failure if initialisation fails")
        void failOnInitialisationFailure() {
            final var exception = new RuntimeException(randomString());
            when(engine.init(anyString(), anyString(), any(), any()))
                    .thenReturn(Try.failure(exception));

            StepVerifier.create(testee.createGameState(id, request))
                        .verifyErrorSatisfies(error -> assertThat(error).isEqualTo(exception));
        }
    }

    @Nested
    @DisplayName("on executeAction should")
    class ExecuteActionTest {
        private final GameStateService.ActionRequest someAction = new GameStateService.ActionRequest(randomString(),
                                                                                                     TurnAction.PROCEED,
                                                                                                     null);
        private final String playerId = randomString();
        private final String payload = randomString();

        @Test
        @DisplayName("execute on engine with gameState from repository")
        void retrieveState() {
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), someAction))
                        .assertNext(GameStateServiceTest::anyNext)
                        .verifyComplete();

            verify(engine).execute(eq(gameState1), any());
        }

        @Test
        @DisplayName("return empty if gameState does not exist")
        void returnEmptyOnNotExisting() {
            StepVerifier.create(testee.executeAction(gameState1.id(), someAction))
                        .expectNext(Optional.empty())
                        .verifyComplete();
        }

        @ParameterizedTest
        @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROCEED", "CANCEL_PROVIDED_HINT", "CANCEL_GAME"})
        @DisplayName("compile action request requiring strings in the expected action")
        void compileStringActions(TurnAction turnAction) {
            final var actionRequest = new GameStateService.ActionRequest(
                    playerId,
                    turnAction,
                    new TextNode(payload)
            );
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), actionRequest))
                        .assertNext(GameStateServiceTest::anyNext)
                        .verifyComplete();

            verify(engine).execute(any(), eq(new Action<>(playerId, turnAction, String.class, payload)));
        }

        @ParameterizedTest
        @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.INCLUDE, names = {"PROCEED", "CANCEL_PROVIDED_HINT", "CANCEL_GAME"})
        @DisplayName("compile action request requiring strings in the expected action")
        void compileVoidActions(TurnAction turnAction) {
            final var actionRequest = new GameStateService.ActionRequest(
                    playerId,
                    turnAction,
                    null
            );
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), actionRequest))
                        .assertNext(GameStateServiceTest::anyNext)
                        .verifyComplete();

            verify(engine).execute(any(), eq(new Action<>(playerId, turnAction, Void.class, null)));
        }

        @ParameterizedTest
        @DisplayName("fail if string action payload is not textual")
        @EnumSource(value = TurnAction.class, mode = EnumSource.Mode.EXCLUDE, names = {"PROCEED", "CANCEL_PROVIDED_HINT", "CANCEL_GAME"})
        void failOnNotTextForStringActions(TurnAction turnAction) {
            final var actionRequest = new GameStateService.ActionRequest(
                    playerId,
                    turnAction,
                    null
            );
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), actionRequest))
                        .verifyErrorSatisfies(error ->
                                                      assertThat(error)
                                                              .isInstanceOf(IllegalArgumentException.class)
                                                              .hasMessage(
                                                                      "expected a string payload got 'null' instead"));

            verify(engine, never()).execute(any(), any());
        }

        @Test
        @DisplayName("return the game state from the engine execution")
        void returnGameState() {
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), someAction))
                        .expectNext(Optional.of(gameState2))
                        .verifyComplete();
        }

        @Test
        @DisplayName("save the game state in the repository")
        void saveGameState() {
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.success(gameState2));

            StepVerifier.create(testee.executeAction(gameState1.id(), someAction))
                        .expectNext(Optional.of(gameState2))
                        .verifyComplete();

            assertThat(gameStateRepository.findById(gameState2.id()))
                    .contains(gameState2);
        }

        @Test
        @DisplayName("return failure if execution fails")
        void failureOnExecutionFailure() {
            final var exception = new RuntimeException(randomString());
            gameStateRepository.save(gameState1);
            when(engine.execute(any(), any())).thenReturn(Try.failure(exception));

            StepVerifier.create(testee.executeAction(gameState1.id(), someAction))
                        .verifyErrorSatisfies(error -> assertThat(error).isEqualTo(exception));
        }
    }
}