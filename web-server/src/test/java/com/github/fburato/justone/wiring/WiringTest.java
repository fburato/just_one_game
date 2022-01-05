package com.github.fburato.justone.wiring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = Wiring.class)
class WiringTest {

    @Autowired
    private RouterFunction<ServerResponse> routerFunction;

    @Test
    @DisplayName("should wire everything")
    void wireEverything() {
        assertThat(routerFunction).isNotNull();
    }
}