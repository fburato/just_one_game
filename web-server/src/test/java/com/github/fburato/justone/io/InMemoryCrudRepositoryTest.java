package com.github.fburato.justone.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.fburato.justone.RandomUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCrudRepositoryTest {

    private final InMemoryCrudRepository<String, TestEntity> testee = new InMemoryCrudRepository<>(TestEntity::id);
    private final TestEntity testEntity1 = new TestEntity(randomString(), randomString());
    private final TestEntity testEntity2 = new TestEntity(randomString(), randomString());
    private final TestEntity testEntity3 = new TestEntity(randomString(), randomString());

    @Test
    @DisplayName("on findById should return empty if repository is empty")
    void findEmptyOnEmptyRepository() {
        assertThat(testee.findById(randomString()))
                .isEmpty();
    }

    @Test
    @DisplayName("on getAll should return empty if repository is empty")
    void getAllEmptyOnEmptyRepository() {
        assertThat(testee.getAll())
                .isEmpty();
    }

    @Test
    @DisplayName("should find entity by id after save")
    void findByIdAfterSave() {
        testee.save(testEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .contains(testEntity1);
    }

    @Test
    @DisplayName("should return persisted entity on save")
    void returnEntityOnSave() {
        assertThat(testee.save(testEntity1))
                .isEqualTo(testEntity1);
    }

    @Test
    @DisplayName("should find entities by id after saveAll")
    void findByIdAfterSaveAll() {
        testee.saveAll(List.of(testEntity1, testEntity2));

        assertThat(testee.findById(testEntity1.id())).isPresent();
        assertThat(testee.findById(testEntity2.id())).isPresent();
    }

    @Test
    @DisplayName("should return all entities after saving")
    void getAllAfterSave() {
        testee.save(testEntity1);
        testee.saveAll(List.of(testEntity2, testEntity3));

        assertThat(testee.getAll())
                .containsExactlyInAnyOrder(testEntity1, testEntity2, testEntity3);
    }

    @Test
    @DisplayName("should update existing entity on save")
    void updateEntity() {
        final var otherTestEntity1 = new TestEntity(testEntity1.id(), randomString());

        assertThat(testEntity1).isNotEqualTo(otherTestEntity1);

        testee.save(testEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .contains(testEntity1);

        testee.save(otherTestEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .contains(otherTestEntity1);
    }

    @Test
    @DisplayName("on delete, should return false if entity does not exist")
    void falseOnUnexistingEntityDeletion() {
        testee.save(testEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .isPresent();

        assertThat(testee.delete(testEntity2.id())).isFalse();
    }

    @Test
    @DisplayName("on delete, should return true if entity exists")
    void trueOnExistingEntityDeletion() {
        testee.save(testEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .isPresent();

        assertThat(testee.delete(testEntity1.id()))
                .isTrue();
    }

    @Test
    @DisplayName("should not return entity after deletion")
    void entityDeletedNoLongerRemoved() {
        testee.save(testEntity1);

        assertThat(testee.findById(testEntity1.id()))
                .isPresent();

        testee.delete(testEntity1.id());

        assertThat(testee.findById(testEntity1.id()))
                .isEmpty();
    }

    @Test
    @DisplayName("should delete only the entity associated to the ids leaving the other there")
    void deleteOnlyById() {
        testee.saveAll(List.of(testEntity1, testEntity2, testEntity3));

        assertThat(testee.getAll()).hasSize(3);

        testee.delete(testEntity1.id());

        assertThat(testee.getAll())
                .containsExactlyInAnyOrder(testEntity2, testEntity3);
    }

}

record TestEntity(String id, String data) {

}