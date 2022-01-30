package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.services.GameStateService;
import io.vavr.control.Validation;

import java.util.List;
import java.util.function.Function;

import static com.github.fburato.justone.controllers.validation.BaseValidators.nonEmptyList;
import static com.github.fburato.justone.controllers.validation.BaseValidators.notBlankStrippedString;

public class CreateStateRequestValidator implements EntityValidator.Validator<GameStateService.CreateStateRequest> {
    @Override
    public Class<GameStateService.CreateStateRequest> validatorType() {
        return GameStateService.CreateStateRequest.class;
    }

    @Override
    public Validation<List<String>, GameStateService.CreateStateRequest> validate(GameStateService.CreateStateRequest value) {
        return Validation.combine(
                        notBlankStrippedString(value.host(), "host"),
                        nonEmptyList(value.players(), "players"),
                        nonEmptyList(value.wordsToGuess(), "wordsToGuess"))
                .ap(GameStateService.CreateStateRequest::new)
                .mapError(s -> s.flatMap(Function.identity()).asJava());
    }
}
