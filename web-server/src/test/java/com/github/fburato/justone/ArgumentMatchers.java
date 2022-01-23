package com.github.fburato.justone;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.argThat;

public class ArgumentMatchers {

    public static <T> T satisfies(Consumer<T> assertionOnT) {
        return argThat(t -> {
            assertionOnT.accept(t);
            return true;
        });
    }
}
