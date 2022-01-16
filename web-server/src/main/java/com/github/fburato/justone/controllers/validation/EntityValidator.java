package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.controllers.ValidationException;
import io.vavr.control.Validation;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityValidator {

    public interface Validator<T> {

        Class<T> validatorType();

        Validation<List<String>, T> validate(T value);
    }

    private final Map<Class<?>, Validator<?>> validators;

    public EntityValidator(List<Validator<?>> validators) {
        this.validators = validators.stream()
                .collect(Collectors.toMap(Validator::validatorType, v -> v));
    }

    public <T> Mono<T> parseBodyAndValidate(ServerRequest serverRequest, Class<T> tClass) {
        return serverRequest.bodyToMono(tClass)
                .flatMap(value -> {
                    final var validator = validatorFor(tClass);
                    final var validationResult = validator.validate(value);
                    if (validationResult.isValid()) {
                        return Mono.just(validationResult.get());
                    }
                    return Mono.error(new ValidationException(validationResult.getError()));
                });
    }

    @SuppressWarnings("unchecked")
    private <T> Validator<T> validatorFor(Class<T> tClass) {
        return Optional.ofNullable((Validator<T>) validators.get(tClass))
                .orElseGet(() -> new Validator<>() {
                    @Override
                    public Class<T> validatorType() {
                        return tClass;
                    }

                    @Override
                    public Validation<List<String>, T> validate(T value) {
                        return Validation.valid(value);
                    }
                });
    }
}
