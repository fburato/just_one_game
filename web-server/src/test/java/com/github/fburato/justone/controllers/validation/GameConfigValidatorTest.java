package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.model.GameConfig;
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

class GameConfigValidatorTest {

    private final EntityValidator.Validator<GameConfig> testee = new GameConfigValidator();
    private final String gameId = randomString();
    private final String host = randomString();
    private final String languageId = randomString();
    private final List<String> wordPackNames = List.of(randomString(), randomString());

    static Stream<Arguments> blankStrings() {
        return Stream.of(
                Arguments.of((Object[]) new String[]{null}),
                Arguments.of(""),
                Arguments.of("    ")
        );
    }

    public static Stream<Arguments> blankWordPackNames() {
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
    @DisplayName("should validate GameConfigs")
    void validationClass() {
        assertThat(testee.validatorType()).isEqualTo(GameConfig.class);
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    @DisplayName("blank gameId should be invalid")
    void blankGameId(String gameId) {
        final var validationResult = testee.validate(new GameConfig(gameId, host, languageId, wordPackNames));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(String.format("gameId='%s' is blank while it should be defined", gameId));
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    @DisplayName("blank host should be invalid")
    void blankHost(String host) {
        final var validationResult = testee.validate(new GameConfig(gameId, host, languageId, wordPackNames));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(String.format("host='%s' is blank while it should be defined", host));
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    @DisplayName("blank languageId should be invalid")
    void blankLanguageId(String languageId) {
        final var validationResult = testee.validate(new GameConfig(gameId, host, languageId, wordPackNames));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(String.format("languageId='%s' is blank while it should be defined", languageId));
    }

    @ParameterizedTest
    @MethodSource("blankWordPackNames")
    @DisplayName("blankWordPacks should be invalid")
    void blankWordPacks(List<String> wordPackNames) {
        final var validationResult = testee.validate(new GameConfig(gameId, host, languageId, wordPackNames));

        assertThat(validationResult.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("should accumulate validation errors")
    void accumulateValidationErrors() {
        final var validationResult = testee.validate(new GameConfig(null, "", "   ", List.of("")));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(
                "gameId='null' is blank while it should be defined",
                "host='' is blank while it should be defined",
                "languageId='   ' is blank while it should be defined",
                "element='' of wordPackNames is blank while it should be defined"
        );
    }

    @Test
    @DisplayName("should return stripped version of all data if valid")
    void returnStrippedValid() {
        final var validationResult = testee.validate(new GameConfig("  " + gameId,
                host + "  ",
                "   " + languageId + "   ",
                List.of("  " + wordPackNames.get(0), "   " + wordPackNames.get(1) + "   ")));

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.get()).isEqualTo(new GameConfig(gameId, host, languageId, wordPackNames));
    }
}