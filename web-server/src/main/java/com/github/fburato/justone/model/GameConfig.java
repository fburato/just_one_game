package com.github.fburato.justone.model;

import java.util.List;

public record GameConfig(String gameId, String host, String languageId, List<String> wordPackNames) {
}
