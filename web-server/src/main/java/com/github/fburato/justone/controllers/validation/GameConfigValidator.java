package com.github.fburato.justone.controllers.validation;

import com.github.fburato.justone.model.GameConfig;
import io.vavr.control.Validation;

import java.util.List;
import java.util.function.Function;

import static com.github.fburato.justone.controllers.validation.BaseValidators.nonEmptyList;
import static com.github.fburato.justone.controllers.validation.BaseValidators.notBlankStrippedString;

public class GameConfigValidator implements EntityValidator.Validator<GameConfig> {

    @Override
    public Class<GameConfig> validatorType() {
        return GameConfig.class;
    }

    @Override
    public Validation<List<String>, GameConfig> validate(GameConfig value) {
        return Validation.combine(
                        notBlankStrippedString(value.gameId(), "gameId"),
                        notBlankStrippedString(value.host(), "host"),
                        notBlankStrippedString(value.languageId(), "languageId"),
                        nonEmptyList(value.wordPackNames(), "wordPackNames"))
                .ap(GameConfig::new)
                .mapError(s -> s.flatMap(Function.identity()).asJava());
    }
}
