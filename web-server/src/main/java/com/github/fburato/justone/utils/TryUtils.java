package com.github.fburato.justone.utils;

import io.vavr.control.Try;
import reactor.core.publisher.Mono;

public class TryUtils {

    public static <T> Mono<T> toMono(Try<T> tTry) {
        if (tTry.isFailure()) {
            return Mono.error(tTry.getCause());
        }
        return Mono.just(tTry.get());
    }
}
