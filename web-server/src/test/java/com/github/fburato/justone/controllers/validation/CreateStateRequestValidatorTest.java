package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.services.GameStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class CreateStateRequestValidatorTest {

    private final EntityValidator.Validator<GameStateService.CreateStateRequest> testee = new CreateStateRequestValidator();
    private final String host = randomString();
    private final List<String> players = List.of(randomString(), randomString());
    private final List<String> wordsToGuess = List.of(randomString(), randomString());

    static Stream<Arguments> blankStrings() {
        return Stream.of(
                Arguments.of((Object[]) new String[]{null}),
                Arguments.of(""),
                Arguments.of("    ")
        );
    }

    public static Stream<Arguments> blankStringsInList() {
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

    @Test
    @DisplayName("should validate createStateRequests")
    void validationClass() {
        assertThat(testee.validatorType()).isEqualTo(GameStateService.CreateStateRequest.class);
    }


    @ParameterizedTest
    @MethodSource("blankStrings")
    @DisplayName("blank host should be invalid")
    void blankHost(String host) {
        final var validationResult = testee.validate(
                new GameStateService.CreateStateRequest(host, players, wordsToGuess));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(String.format("host='%s' is blank while it should be defined", host));
    }

    @ParameterizedTest
    @MethodSource("blankStringsInList")
    @DisplayName("blank players should be invalid")
    void blankPlayers(List<String> blankStrings) {
        final var validationResult = testee.validate(
                new GameStateService.CreateStateRequest(host, blankStrings, wordsToGuess));

        assertThat(validationResult.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("blankStringsInList")
    @DisplayName("blank words to guess should be invalid")
    void blankWordsToGuess(List<String> blankStrings) {
        final var validationResult = testee.validate(
                new GameStateService.CreateStateRequest(host, players, blankStrings));

        assertThat(validationResult.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("should accumulate validation errors")
    void accumulateValidationErrors() {
        final var validationResult = testee.validate(new GameStateService.CreateStateRequest(null, List.of(), List.of()));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(
                "host='null' is blank while it should be defined",
                "players=[] is an empty-list while it should be non-empty",
                "wordsToGuess=[] is an empty-list while it should be non-empty"
        );
    }

    @Test
    @DisplayName("should return stripped version of all data if valid")
    void returnStrippedValid() {
        final var validationResult = testee.validate(
                new GameStateService.CreateStateRequest(
                        host + "  ",
                        List.of("   " + players.get(0) + "   ", players.get(1) + "   "),
                        List.of("  " + wordsToGuess.get(0), "   " + wordsToGuess.get(1) + "   ")));

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.get()).isEqualTo(new GameStateService.CreateStateRequest(host, players, wordsToGuess));
    }
}