package com.github.fburato.justone.utils;

import java.util.stream.Stream;

public class StreamUtils {

    public static <T, S extends T> Stream<T> append(Stream<T> stream, S ... elements) {
        return Stream.concat(stream, Stream.of(elements));
    }

    public static <T, S extends T> Stream<T> prepend(Stream<T> stream, S ... elements) {
        return Stream.concat(Stream.of(elements), stream);
    }
}
