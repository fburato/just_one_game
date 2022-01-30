package com.github.fburato.justone.controllers.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fburato.justone.model.TurnAction;
import com.github.fburato.justone.services.GameStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.github.fburato.justone.RandomUtils.randomEnum;
import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class ActionRequestValidatorTest {

    private final ActionRequestValidator testee = new ActionRequestValidator();
    private final String playerId = randomString();
    private final TurnAction turnAction = randomEnum(TurnAction.class);
    private final JsonNode payload = new TextNode(randomString());

    @Test
    @DisplayName("should validate ActionRequest")
    void validateActionRequest() {
        assertThat(testee.validatorType()).isEqualTo(GameStateService.ActionRequest.class);
    }

    @ParameterizedTest
    @MethodSource("com.github.fburato.justone.controllers.validation.ValidationValues#blankStrings")
    @DisplayName("blank playerId should be invalid")
    void blankPlayerId(String playerId) {
        final var validationResult = testee.validate(new GameStateService.ActionRequest(
                playerId, turnAction, payload
        ));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly(String.format("playerId='%s' is blank while it should be defined", playerId));
    }

    @Test
    @DisplayName("null turnAction should be invalid")
    void nullTurnAction() {
        final var validationResult = testee.validate(new GameStateService.ActionRequest(
                playerId, null, payload
        ));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactly("turnAction should not be null");
    }

    @Test
    @DisplayName("null payload should be valid and converted to defined NullNode")
    void nullPayload() {
        final var validationResult = testee.validate(new GameStateService.ActionRequest(
                playerId, turnAction, null
        ));

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.get().payload()).isEqualTo(NullNode.getInstance());
    }

    @Test
    @DisplayName("should accumulate error")
    void accumulateErrors() {
        final var validationResult = testee.validate(new GameStateService.ActionRequest(
                null, null, payload
        ));

        assertThat(validationResult.isInvalid()).isTrue();
        assertThat(validationResult.getError()).containsExactlyInAnyOrder(
                "turnAction should not be null",
                "playerId='null' is blank while it should be defined"
        );
    }

    @Test
    @DisplayName("should return stripped version of all data if valid")
    void returnStrippedValues() {
        final var validationResult = testee.validate(new GameStateService.ActionRequest(
                "   " + playerId + "    ", turnAction, payload
        ));

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.get()).isEqualTo(new GameStateService.ActionRequest(playerId, turnAction, payload));
    }

}