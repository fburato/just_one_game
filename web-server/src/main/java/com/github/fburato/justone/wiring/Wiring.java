package com.github.fburato.justone.wiring;

import com.github.fburato.justone.controllers.GameStateController;
import org.springframework.context.annotation.Bean;

public class Wiring {
    @Bean
    public GameStateController gameStateController() {
        return new GameStateController();
    }
}
