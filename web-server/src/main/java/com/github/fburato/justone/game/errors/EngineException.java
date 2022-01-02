package com.github.fburato.justone.game.errors;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EngineException extends RuntimeException {

    private final List<ErrorCode> errorCodes;

    public EngineException() {
        super();
        this.errorCodes = List.of();
    }

    public EngineException(String message) {
        super(message);
        this.errorCodes = List.of();
    }

    public EngineException(ErrorCode... errorCodes) {
        super(Stream.of(errorCodes)
                    .map(ErrorCode::name)
                    .collect(Collectors.joining(", ")));
        this.errorCodes = List.of(errorCodes);
    }

    public List<ErrorCode> errorCodes() {
        return errorCodes;
    }
}
