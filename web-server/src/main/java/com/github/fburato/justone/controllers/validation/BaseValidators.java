package com.github.fburato.justone.controllers.validation;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

class BaseValidators {
    static Validation<Seq<String>, List<String>> nonEmptyList(List<String> content, String fieldName) {
        if (content == null) {
            return Validation.invalid(io.vavr.collection.List.of(String.format("%s=%s is null while it should be a non-empty list", fieldName, content)));
        }
        if (content.isEmpty()) {
            return Validation.invalid(io.vavr.collection.List.of(String.format("%s=%s is an empty-list while it should be non-empty", fieldName, content)));
        }
        final List<String> errors = new ArrayList<>();
        final List<String> trimmedContent = new ArrayList<>();
        for (String element : content) {
            final var trimmed = StringUtils.strip(element);
            if (StringUtils.isBlank(trimmed)) {
                errors.add(String.format("element='%s' of %s is blank while it should be defined", element, fieldName));
            } else {
                trimmedContent.add(trimmed);
            }
        }
        if (errors.isEmpty()) {
            return Validation.valid(trimmedContent);
        }
        return Validation.invalid(io.vavr.collection.List.ofAll(errors));
    }

    static Validation<Seq<String>, String> notBlankStrippedString(String value, String fieldName) {
        final var trimmed = StringUtils.strip(value);
        if (StringUtils.isBlank(trimmed)) {
            return Validation.invalid(io.vavr.collection.List.of(String.format("%s='%s' is blank while it should be defined", fieldName, value)));
        }
        return Validation.valid(trimmed);
    }
}
