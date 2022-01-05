package com.github.fburato.justone.wiring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class LoggingFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final var request = exchange.getRequest();
        final var method = request.getMethod();
        final var path = request.getURI().getPath();
        final String requestId = Optional.ofNullable(request.getHeaders().get("x-request-id"))
                                         .flatMap(l -> l.stream().findFirst())
                                         .orElseGet(() -> UUID.randomUUID().toString());

        final var mutatedRequest = request.mutate()
                                          .header("x-request-id", requestId)
                                          .build();
        final var mutatedExchange = exchange.mutate()
                                            .request(mutatedRequest)
                                            .build();
        LOG.info("> {} {} - requestId={}", method, path, requestId);
        final var start = Instant.now();
        final var responseResult = chain.filter(mutatedExchange);
        mutatedExchange.getResponse().getHeaders().add("x-request-id", requestId);
        exchange.getResponse().beforeCommit(() -> {
            final var duration = Duration.between(start, Instant.now());
            final var response = exchange.getResponse();
            LOG.info("< {} {} - status={} duration={} requestId={}", request.getMethod(), request.getPath(),
                     response.getRawStatusCode(), duration.toMillis(), requestId);
            return Mono.empty();
        });
        return responseResult;
    }
}
