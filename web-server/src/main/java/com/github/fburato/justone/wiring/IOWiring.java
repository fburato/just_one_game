package com.github.fburato.justone.wiring;

import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.io.InMemoryCrudRepository;
import com.github.fburato.justone.model.GameConfig;
import com.github.fburato.justone.model.GameState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IOWiring {

    @Bean
    public CrudRepository<String, GameState> gameStateRepository() {
        return new InMemoryCrudRepository<>(GameState::id);
    }

    @Bean
    public CrudRepository<String, GameConfig> gameConfigRepository() {
        return new InMemoryCrudRepository<>(GameConfig::gameId);
    }
}
