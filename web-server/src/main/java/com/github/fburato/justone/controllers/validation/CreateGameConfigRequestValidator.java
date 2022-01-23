package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.controllers.GameConfigController;
import io.vavr.control.Validation;

import java.util.List;
import java.util.function.Function;

import static com.github.fburato.justone.controllers.validation.BaseValidators.nonEmptyList;
import static com.github.fburato.justone.controllers.validation.BaseValidators.notBlankStrippedString;

public class CreateGameConfigRequestValidator implements EntityValidator.Validator<GameConfigController.CreateGameConfigRequest> {
    @Override
    public Class<GameConfigController.CreateGameConfigRequest> validatorType() {
        return GameConfigController.CreateGameConfigRequest.class;
    }

    @Override
    public Validation<List<String>, GameConfigController.CreateGameConfigRequest> validate(GameConfigController.CreateGameConfigRequest value) {
        return Validation.combine(
                        notBlankStrippedString(value.host(), "host"),
                        notBlankStrippedString(value.languageId(), "languageId"),
                        nonEmptyList(value.wordPackNames(), "wordPackNames"))
                .ap(GameConfigController.CreateGameConfigRequest::new)
                .mapError(s -> s.flatMap(Function.identity()).asJava());
    }
}
