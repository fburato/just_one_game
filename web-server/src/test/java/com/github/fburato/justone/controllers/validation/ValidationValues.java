package com.github.fburato.justone.controllers.validation;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;

class ValidationValues {

    static Stream<Arguments> blankStrings() {
        return Stream.of(
                Arguments.of((Object[]) new String[]{null}),
                Arguments.of(""),
                Arguments.of("    ")
        );
    }

    static Stream<Arguments> blankStringsInList() {
        final List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(List.of("")),
                Arguments.of(List.of(randomString(), "")),
                Arguments.of(List.of("   ", randomString())),
                Arguments.of(listWithNulls)
        );
    }
}
