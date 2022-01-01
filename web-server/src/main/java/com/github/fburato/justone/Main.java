package com.github.fburato.justone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(Wiring.class)
public class Main {
    public static void main(String[] argv) {
        SpringApplication.run(Main.class, argv);
    }
}
