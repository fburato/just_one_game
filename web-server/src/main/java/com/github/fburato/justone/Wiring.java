package com.github.fburato.justone;

import org.springframework.context.annotation.Bean;

public class Wiring {
    @Bean
    public GameStateController gameStateController() {
        return new GameStateController();
    }
}
