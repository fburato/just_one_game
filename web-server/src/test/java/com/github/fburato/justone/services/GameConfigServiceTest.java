package com.github.fburato.justone.services;

import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.io.InMemoryCrudRepository;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
import com.github.fburato.justone.services.errors.GameConfigConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static com.github.fburato.justone.RandomUtils.randomGameConfig;
import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class GameConfigServiceTest {

    private final CrudRepository<String, GameConfig> gameConfigCrudRepository = new InMemoryCrudRepository<>(GameConfig::gameId);
    private final GameConfigService testee = new GameConfigService(gameConfigCrudRepository);

    private final GameConfig gameConfig1 = randomGameConfig();
    private final GameConfig gameConfig2 = randomGameConfig();

    private static <T> void anyNext(T entity) {

    }

    @Nested
    @DisplayName("on getGameConfig should")
    class GetGameConfigTests {

        @Test
        @DisplayName("return empty if repository returns empty")
        void emptyOnEmpty() {
            StepVerifier.create(testee.getGameConfig(gameConfig1.gameId()))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("return game config if present in the repository")
        void gameConfigOnDefined() {
            gameConfigCrudRepository.save(gameConfig1);

            StepVerifier.create(testee.getGameConfig(gameConfig1.gameId()))
                    .expectNext(Optional.of(gameConfig1))
                    .verifyComplete();
        }

        @Test
        @DisplayName("return the game config with the requested id if multiple are present")
        void requestedConfig() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));

            StepVerifier.create(testee.getGameConfig(gameConfig2.gameId()))
                    .expectNext(Optional.of(gameConfig2))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on createGameConfig should")
    class CreateGameConfigTests {
        @Test
        @DisplayName("fail with GameConfigConflictException if config with the same id already exists")
        void conflictOnAlreadyExisting() {
            gameConfigCrudRepository.save(gameConfig1);

            StepVerifier.create(testee.createGameConfig(gameConfig1))
                    .verifyErrorSatisfies(t ->
                            assertThat(t).isInstanceOfSatisfying(GameConfigConflictException.class, e ->
                                    assertThat(e.entityId()).isEqualTo(gameConfig1.gameId())));
        }

        @Test
        @DisplayName("define the game config in the repository")
        void defineConfig() {

            StepVerifier.create(testee.createGameConfig(gameConfig1))
                    .assertNext(GameConfigServiceTest::anyNext)
                    .verifyComplete();

            assertThat(gameConfigCrudRepository.findById(gameConfig1.gameId()))
                    .contains(gameConfig1);
        }

        @Test
        @DisplayName("return the game config to the user on success")
        void returnConfig() {
            StepVerifier.create(testee.createGameConfig(gameConfig1))
                    .expectNext(gameConfig1)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on updateGameConfig should")
    class UpdateGameConfigTests {

        @Test
        @DisplayName("return empty if game config for id does not exist")
        void emptyOnNotExisting() {
            StepVerifier.create(testee.updateGameConfig(gameConfig1.gameId(), gameConfig1))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("fail with EntityIdMismatchException if game id of the resource does not match game id of the entity")
        void idMismatchFailure() {
            final var otherId = randomString();

            StepVerifier.create(testee.updateGameConfig(otherId, gameConfig1))
                    .verifyErrorSatisfies(t ->
                            assertThat(t).isInstanceOfSatisfying(EntityIdMismatchException.class, e -> {
                                assertThat(e.entityId()).isEqualTo(gameConfig1.gameId());
                                assertThat(e.resourceId()).isEqualTo(otherId);
                            }));
        }

        @Test
        @DisplayName("update the entity on matching id")
        void updateEntityMatchin() {
            gameConfigCrudRepository.save(gameConfig1);
            final var otherGameConfig1 = new GameConfig(gameConfig1.gameId(), randomString(), randomString(), List.of(randomString()));

            StepVerifier.create(testee.updateGameConfig(gameConfig1.gameId(), otherGameConfig1))
                    .assertNext(GameConfigServiceTest::anyNext)
                    .verifyComplete();

            assertThat(gameConfigCrudRepository.findById(gameConfig1.gameId()))
                    .contains(otherGameConfig1);
        }

        @Test
        @DisplayName("not update any other entity")
        void noOtherUpdate() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));
            final var otherGameConfig1 = new GameConfig(gameConfig1.gameId(), randomString(), randomString(), List.of(randomString()));

            StepVerifier.create(testee.updateGameConfig(gameConfig1.gameId(), otherGameConfig1))
                    .assertNext(GameConfigServiceTest::anyNext)
                    .verifyComplete();

            assertThat(gameConfigCrudRepository.findById(gameConfig2.gameId()))
                    .contains(gameConfig2);
        }

        @Test
        @DisplayName("return updated entity")
        void returnEntity() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));
            final var otherGameConfig1 = new GameConfig(gameConfig1.gameId(), randomString(), randomString(), List.of(randomString()));

            StepVerifier.create(testee.updateGameConfig(gameConfig1.gameId(), otherGameConfig1))
                    .expectNext(Optional.of(otherGameConfig1))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on deleteGameConfig should")
    class DeleteGameConfigTests {

        @Test
        @DisplayName("return empty if game does not exist")
        void emptyOnNotExisting() {
            StepVerifier.create(testee.deleteGameConfig(gameConfig1.gameId()))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("remove the selected game from the repository")
        void removeGameFromRepo() {

            gameConfigCrudRepository.save(gameConfig1);

            StepVerifier.create(testee.deleteGameConfig(gameConfig1.gameId()))
                    .assertNext(GameConfigServiceTest::anyNext)
                    .verifyComplete();

            assertThat(gameConfigCrudRepository.findById(gameConfig1.gameId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("not remove any other game but the selected one")
        void noOtherRemoval() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));

            StepVerifier.create(testee.deleteGameConfig(gameConfig1.gameId()))
                    .assertNext(GameConfigServiceTest::anyNext)
                    .verifyComplete();

            assertThat(gameConfigCrudRepository.findById(gameConfig2.gameId()))
                    .isPresent();
        }

        @Test
        @DisplayName("return the removed game")
        void returnRemoved() {
            gameConfigCrudRepository.save(gameConfig1);

            StepVerifier.create(testee.deleteGameConfig(gameConfig1.gameId()))
                    .expectNext(Optional.of(gameConfig1))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on getAllGameConfigs should")
    class GetAllGameConfigsTests {

        @Test
        @DisplayName("return all the saved games")
        void returnAllGames() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));

            StepVerifier.create(testee.getAllGameConfigs().buffer(2))
                    .assertNext(res -> assertThat(res).containsExactlyInAnyOrder(gameConfig1, gameConfig2))
                    .verifyComplete();
        }

        @Test
        @DisplayName("return empty if no game config defined")
        void emptyOnNoGameConfigs() {
            StepVerifier.create(testee.getAllGameConfigs())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("on getAllGameConfigIds should")
    class GetAllGameConfigIdsTests {
        @Test
        @DisplayName("return all the saved games ids")
        void returnAllGameIds() {
            gameConfigCrudRepository.saveAll(List.of(gameConfig1, gameConfig2));

            StepVerifier.create(testee.getAllGameConfigIds().buffer(2))
                    .assertNext(res -> assertThat(res).containsExactlyInAnyOrder(gameConfig1.gameId(), gameConfig2.gameId()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("return empty if no game config defined")
        void emptyOnNoGameConfigs() {
            StepVerifier.create(testee.getAllGameConfigIds())
                    .verifyComplete();
        }
    }
}