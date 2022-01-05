package com.github.fburato.justone.io;

import com.github.fburato.justone.internals.CrudRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryCrudRepository<ID, E> implements CrudRepository<ID, E> {

    private final Function<E, ID> idExtractor;
    private final Map<ID, E> registry = new ConcurrentHashMap<>();

    public InMemoryCrudRepository(Function<E, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public ID idExtractor(E entity) {
        return idExtractor.apply(entity);
    }

    @Override
    public Optional<E> findById(ID id) {
        return Optional.ofNullable(registry.get(id));
    }

    @Override
    public Stream<E> getAll() {
        return registry.values().stream();
    }

    @Override
    public E save(E entity) {
        final var id = idExtractor(entity);
        registry.put(id, entity);
        return entity;
    }

    @Override
    public void saveAll(Collection<E> entities) {
        entities.forEach(this::save);
    }

    @Override
    public boolean delete(ID id) {
        if (registry.containsKey(id)) {
            registry.remove(id);
            return true;
        }
        return false;
    }
}
