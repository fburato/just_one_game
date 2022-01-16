package com.github.fburato.justone.controllers;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public ValidationException(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<String> validationErrors() {
        return validationErrors;
    }
}
