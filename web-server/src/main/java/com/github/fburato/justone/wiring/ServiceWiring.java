package com.github.fburato.justone.wiring;

import com.github.fburato.justone.game.engine.Engine;
import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.model.GameState;
import com.github.fburato.justone.services.GameConfigService;
import com.github.fburato.justone.services.GameStateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceWiring {
    @Bean
    public GameStateService gameStateService(Engine engine, CrudRepository<String, GameState> gameStateCrudRepository) {
        return new GameStateService(engine, gameStateCrudRepository);
    }

    @Bean
    public GameConfigService gameConfigService(CrudRepository<String, GameConfig> gameConfigCrudRepository) {
        return new GameConfigService(gameConfigCrudRepository);
    }
}
