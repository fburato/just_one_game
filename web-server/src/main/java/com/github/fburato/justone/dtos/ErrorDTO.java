package com.github.fburato.justone.dtos;

import java.util.List;

public record ErrorDTO(String message, List<Integer> errorCodes) {
}
