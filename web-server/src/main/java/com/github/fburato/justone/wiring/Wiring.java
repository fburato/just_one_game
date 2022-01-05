package com.github.fburato.justone.wiring;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ControllerWiring.class, CoreWiring.class, IOWiring.class, ServiceWiring.class})
public class Wiring {
}