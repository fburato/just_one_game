package com.github.fburato.justone.dtos;

import java.util.List;

public record ErrorDTO(List<String> messages, List<Integer> errorCodes) {

    public ErrorDTO(String message, List<Integer> errorCodes) {
        this(List.of(message), errorCodes);
    }

    public ErrorDTO(String message) {
        this(message, List.of());
    }
}
