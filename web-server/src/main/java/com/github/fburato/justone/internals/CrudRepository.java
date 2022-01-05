package com.github.fburato.justone.internals;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface CrudRepository<ID, E> {

    ID idExtractor(E entity);

    Optional<E> findById(ID id);

    Stream<E> getAll();

    E save(E entity);

    void saveAll(Collection<E> entities);

    boolean delete(ID id);
}
