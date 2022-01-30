package com.github.fburato.justone.controllers.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.fburato.justone.services.GameStateService;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;

import java.util.List;
import java.util.function.Function;

import static com.github.fburato.justone.controllers.validation.BaseValidators.notBlankStrippedString;
import static com.github.fburato.justone.controllers.validation.BaseValidators.notNull;

public class ActionRequestValidator implements EntityValidator.Validator<GameStateService.ActionRequest> {

    @Override
    public Class<GameStateService.ActionRequest> validatorType() {
        return GameStateService.ActionRequest.class;
    }

    @Override
    public Validation<List<String>, GameStateService.ActionRequest> validate(GameStateService.ActionRequest value) {
        return Validation.combine(
                        notBlankStrippedString(value.playerId(), "playerId"),
                        notNull(value.turnAction(), "turnAction"),
                        normalisedNode(value.payload()))
                .ap(GameStateService.ActionRequest::new)
                .mapError(s -> s.flatMap(Function.identity()).asJava());
    }

    private Validation<Seq<String>, JsonNode> normalisedNode(JsonNode node) {
        if (node == null) {
            return Validation.valid(NullNode.getInstance());
        }
        return Validation.valid(node);
    }
}
