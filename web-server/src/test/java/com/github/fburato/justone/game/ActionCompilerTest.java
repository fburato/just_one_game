package com.github.fburato.justone.game;


import com.github.fburato.justone.game.errors.ErrorCode;
import com.github.fburato.justone.game.errors.InvalidActionException;
import com.github.fburato.justone.model.Action;
import com.github.fburato.justone.model.TurnAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class ActionCompilerTest {
    private final static Unknown UNKNOWN_VALUE = new Unknown();
    private final ActionCompiler testee = ActionCompiler.DEFAULT_ACTION_COMPILER;

    public static Stream<Arguments> validTypes() {
        final Function<TurnAction, Arguments> stringActionMaker = turnAction -> Arguments.of(turnAction,
                                                                                             new Action<>(
                                                                                                     randomString(),
                                                                                                     turnAction,
                                                                                                     String.class,
                                                                                                     randomString()));
        return Stream.of(
                stringActionMaker.apply(TurnAction.PROVIDE_HINT),
                stringActionMaker.apply(TurnAction.REMOVE_HINT),
                stringActionMaker.apply(TurnAction.ADMIT_PLAYER),
                stringActionMaker.apply(TurnAction.GUESS_WORD),
                stringActionMaker.apply(TurnAction.KICK_PLAYER),
                Arguments.of(TurnAction.PROCEED,
                             new Action<>(randomString(), TurnAction.PROCEED, Void.class, null)),
                Arguments.of(TurnAction.CANCEL_GAME,
                             new Action<>(randomString(), TurnAction.CANCEL_GAME, Void.class, null)),
                Arguments.of(TurnAction.CANCEL_PROVIDED_HINT,
                             new Action<>(randomString(), TurnAction.CANCEL_PROVIDED_HINT, Void.class, null)),
                Arguments.of(TurnAction.CANCEL_REMOVED_HINT,
                             new Action<>(randomString(), TurnAction.CANCEL_REMOVED_HINT, Void.class, null))

        );
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(TurnAction.class)
    @DisplayName("should return INVALID_PAYLOAD error for payload incorrect for the action")
    void invalidPayload(TurnAction turnAction) {
        final var action = new Action<>(randomString(), turnAction, Unknown.class, UNKNOWN_VALUE);

        final var compiledAction = testee.compile(action);
        assertThat(compiledAction.isFailure()).isTrue();
        assertThat(compiledAction.getCause())
                .isInstanceOfSatisfying(InvalidActionException.class, exc -> assertThat(exc.errorCodes())
                        .contains(ErrorCode.INVALID_PAYLOAD));
    }

    @Test
    @DisplayName("should return PAYLOAD_TYPE_MISMATCH error for payload not matching the payload type")
    void payloadMismatch() {
        final var action = new Action(randomString(), TurnAction.CANCEL_PROVIDED_HINT, String.class, UNKNOWN_VALUE);

        final var compiledAction = testee.compile(action);
        assertThat(compiledAction.isFailure()).isTrue();
        assertThat(compiledAction.getCause())
                .isInstanceOfSatisfying(InvalidActionException.class, exc -> assertThat(exc.errorCodes())
                        .contains(ErrorCode.PAYLOAD_TYPE_MISMATCH));
    }

    @Test
    @DisplayName("should return invalid payload and payload type mismatch if they occur at the same time")
    void multipleErrorCodes() {
        final var action = new Action(randomString(), TurnAction.CANCEL_PROVIDED_HINT, Integer.class, UNKNOWN_VALUE);

        final var compiledAction = testee.compile(action);
        assertThat(compiledAction.isFailure()).isTrue();
        assertThat(compiledAction.getCause())
                .isInstanceOfSatisfying(InvalidActionException.class, exc -> assertThat(exc.errorCodes())
                        .containsExactlyInAnyOrder(ErrorCode.PAYLOAD_TYPE_MISMATCH, ErrorCode.INVALID_PAYLOAD));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTypes")
    @DisplayName("should not return action if payload has the expected type")
    void expectedTypes(TurnAction turnAction, Action<?> action) {
        final var compiledAction = testee.compile(action);

        assertThat(compiledAction.isFailure()).isFalse();
        assertThat(compiledAction.get()).isEqualTo(action);
    }

    private static class Unknown {
    }

}