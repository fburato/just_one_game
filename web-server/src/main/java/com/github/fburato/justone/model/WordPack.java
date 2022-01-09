package com.github.fburato.justone.model;

import java.util.List;

public record WordPack(Id wordPackId, List<String> words) {
    public record Id(String name, String language) {

    }
}
