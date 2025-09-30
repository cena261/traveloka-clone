package com.cena.traveloka.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for BaseEntity functionality.
 * Tests UUID generation, version management, and optimistic locking.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * BaseEntity implementation does not exist yet.
 */
@DataJpaTest
@ActiveProfiles("test")
class BaseEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private TestBaseEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestBaseEntity();
        testEntity.setName("Test Entity");
    }

    @Test
    void shouldGenerateUuidOnPersist() {
        // Given: New entity extending BaseEntity
        assertThat(testEntity.getId()).isNull();

        // When: Entity is persisted
        TestBaseEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: UUID is generated automatically
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isInstanceOf(UUID.class);
    }

    @Test
    void shouldInitializeVersionToZero() {
        // Given: New entity
        assertThat(testEntity.getVersion()).isNull();

        // When: Entity is persisted
        TestBaseEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: Version starts at 0
        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    void shouldIncrementVersionOnUpdate() {
        // Given: Existing persisted entity
        TestBaseEntity saved = entityManager.persistAndFlush(testEntity);
        Long initialVersion = saved.getVersion();
        entityManager.clear(); // Clear first-level cache

        // When: Entity is updated
        TestBaseEntity found = entityManager.find(TestBaseEntity.class, saved.getId());
        found.setName("Updated Name");
        TestBaseEntity updated = entityManager.persistAndFlush(found);

        // Then: Version is incremented
        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    void shouldThrowOptimisticLockingException() {
        // Given: Entity persisted and loaded twice
        TestBaseEntity saved = entityManager.persistAndFlush(testEntity);
        entityManager.clear();

        TestBaseEntity entity1 = entityManager.find(TestBaseEntity.class, saved.getId());
        TestBaseEntity entity2 = entityManager.find(TestBaseEntity.class, saved.getId());

        // When: Both entities are updated in different transactions
        entity1.setName("Update 1");
        entityManager.persistAndFlush(entity1);
        entityManager.clear();

        entity2.setName("Update 2");

        // Then: Second update throws OptimisticLockingFailureException
        assertThatThrownBy(() -> entityManager.persistAndFlush(entity2))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void shouldMaintainEntityEquality() {
        // Given: Two entities with same ID
        TestBaseEntity saved = entityManager.persistAndFlush(testEntity);
        UUID entityId = saved.getId();
        entityManager.clear();

        // When: Loading entity again
        TestBaseEntity reloaded = entityManager.find(TestBaseEntity.class, entityId);

        // Then: IDs should be equal
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldGenerateUniqueUuids() {
        // Given: Multiple new entities
        TestBaseEntity entity1 = new TestBaseEntity();
        entity1.setName("Entity 1");
        TestBaseEntity entity2 = new TestBaseEntity();
        entity2.setName("Entity 2");

        // When: Both entities are persisted
        TestBaseEntity saved1 = entityManager.persistAndFlush(entity1);
        TestBaseEntity saved2 = entityManager.persistAndFlush(entity2);

        // Then: UUIDs should be unique
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
    }

    /**
     * Test entity that extends BaseEntity for testing purposes.
     * This will reference the BaseEntity class that doesn't exist yet.
     */
    @Entity
    @Table(name = "test_base_entities")
    public static class TestBaseEntity extends BaseEntity {

        private String name;

        public TestBaseEntity() {
            super();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}