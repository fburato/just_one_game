package com.github.fburato.justone.wiring;

import com.github.fburato.justone.game.engine.ActionCompiler;
import com.github.fburato.justone.game.engine.Engine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreWiring {

    @Bean
    public Engine engine() {
        return new Engine(ActionCompiler.DEFAULT_ACTION_COMPILER);
    }

}
