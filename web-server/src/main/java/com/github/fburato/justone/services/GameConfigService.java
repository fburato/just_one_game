package com.github.fburato.justone.services;

import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
import com.github.fburato.justone.services.errors.GameConfigConflictException;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class GameConfigService {

    private final CrudRepository<String, GameConfig> gameConfigCrudRepository;

    public GameConfigService(CrudRepository<String, GameConfig> gameConfigCrudRepository) {
        this.gameConfigCrudRepository = gameConfigCrudRepository;
    }

    public Mono<Optional<GameConfig>> getGameConfig(String gameId) {

        return Mono.just(gameConfigCrudRepository.findById(gameId));
    }

    public Mono<GameConfig> createGameConfig(GameConfig gameConfig) {

        final var maybeGameId = gameConfigCrudRepository.findById(gameConfig.gameId());
        if (maybeGameId.isPresent()) {
            return Mono.error(new GameConfigConflictException(gameConfig.gameId()));
        }
        return Mono.just(gameConfigCrudRepository.save(gameConfig));
    }

    public Mono<Optional<GameConfig>> updateGameConfig(String gameId, GameConfig gameConfig) {

        if (!StringUtils.equals(gameId, gameConfig.gameId())) {
            return Mono.error(new EntityIdMismatchException(gameId, gameConfig.gameId()));
        }
        return Mono.just(gameConfigCrudRepository.findById(gameId))
                .map(maybeGameConfig -> maybeGameConfig.map(p -> gameConfigCrudRepository.save(gameConfig)));
    }

    public Mono<Optional<GameConfig>> deleteGameConfig(String gameId) {
        final var maybeGameConfig = gameConfigCrudRepository.findById(gameId);
        maybeGameConfig.ifPresent(gc -> gameConfigCrudRepository.delete(gameId));
        return Mono.just(maybeGameConfig);
    }

    public Flux<GameConfig> getAllGameConfigs() {
        return Flux.fromStream(gameConfigCrudRepository.getAll());
    }

    public Flux<String> getAllGameConfigIds() {
        return Flux.fromStream(gameConfigCrudRepository.getAll().map(GameConfig::gameId));
    }
}
