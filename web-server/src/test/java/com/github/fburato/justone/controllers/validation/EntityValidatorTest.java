package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.controllers.ValidationException;
import io.vavr.control.Validation;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class EntityValidatorTest {

    private final String marker1 = randomString();
    private final String marker2 = randomString();

    private final EntityValidator.Validator<String> stringValidator = new EntityValidator.Validator<>() {
        @Override
        public Class<String> validatorType() {
            return String.class;
        }

        @Override
        public Validation<List<String>, String> validate(String value) {
            if (StringUtils.isBlank(value)) {
                return Validation.invalid(List.of(marker1, String.format("%s is blank", value)));
            }
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                return Validation.invalid(List.of(marker2, String.format("%s does not parse to an integer", value)));
            }
            return Validation.valid(value);
        }
    };

    private final EntityValidator.Validator<Integer> integerValidator = new EntityValidator.Validator<>() {
        @Override
        public Class<Integer> validatorType() {
            return Integer.class;
        }

        @Override
        public Validation<List<String>, Integer> validate(Integer value) {
            if (value < 42) {
                return Validation.invalid(List.of(marker2, String.format("%d is less than 42", value)));
            }
            return Validation.valid(value);
        }
    };

    private final EntityValidator testee = new EntityValidator(List.of(stringValidator, integerValidator));

    @Test
    @DisplayName("should fail if body deserialisation fails")
    void failOnDeserialisationFailure() {
        final var request = MockServerRequest.builder().body(Mono.just(42));
        StepVerifier.create(testee.parseBodyAndValidate(request, String.class))
                .verifyErrorSatisfies(t -> assertThat(t).isInstanceOf(ClassCastException.class));
    }

    @Test
    @DisplayName("should deserialise and return the deserialised body if the associated validator validates the value")
    void applyTypeValidatorValid() {
        final var request1 = MockServerRequest.builder().body(Mono.just("12345"));
        StepVerifier.create(testee.parseBodyAndValidate(request1, String.class))
                .expectNext("12345")
                .verifyComplete();

        final var request2 = MockServerRequest.builder().body(Mono.just(62));
        StepVerifier.create(testee.parseBodyAndValidate(request2, Integer.class))
                .expectNext(62)
                .verifyComplete();
    }

    @Test
    @DisplayName("should deserialise and return failed mono with validation exception if validator does not validate the value")
    void applyTypeValidatorInvalid() {
        final var request = MockServerRequest.builder().body(Mono.just("foo"));

        StepVerifier.create(testee.parseBodyAndValidate(request, String.class))
                .verifyErrorSatisfies(t -> assertThat(t).isInstanceOfSatisfying(ValidationException.class, ve ->
                        assertThat(ve.validationErrors()).containsExactly(marker2, "foo does not parse to an integer")));
    }

    @Test
    @DisplayName("should apply registered validators depending on the type")
    void applyDifferentTypeValidators() {
        final var request1 = MockServerRequest.builder().body(Mono.just(""));

        StepVerifier.create(testee.parseBodyAndValidate(request1, String.class))
                .verifyErrorSatisfies(t -> assertThat(t).isInstanceOfSatisfying(ValidationException.class, ve ->
                        assertThat(ve.validationErrors()).containsExactly(marker1, " is blank")));
    }

    @Test
    @DisplayName("should validate types which are not registered")
    void noOpOnUnregistered() {
        final var request = MockServerRequest.builder().body(Mono.just(52.0));

        StepVerifier.create(testee.parseBodyAndValidate(request, Double.class))
                .expectNext(52.0)
                .verifyComplete();
    }

}