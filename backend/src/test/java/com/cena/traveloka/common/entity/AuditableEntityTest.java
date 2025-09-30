package com.cena.traveloka.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

/**
 * Test class for AuditableEntity functionality.
 * Tests automatic timestamp population, audit field management, and soft delete.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * AuditableEntity implementation does not exist yet.
 */
@DataJpaTest
@ActiveProfiles("test")
class AuditableEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    private TestAuditableEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new TestAuditableEntity();
        testEntity.setTitle("Test Auditable Entity");
    }

    @Test
    void shouldSetCreationTimestampOnPersist() {
        // Given: New auditable entity
        ZonedDateTime beforeSave = ZonedDateTime.now(ZoneOffset.UTC);
        assertThat(testEntity.getCreatedAt()).isNull();

        // When: Entity is persisted
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: Creation timestamp is set in UTC
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getCreatedAt().getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(saved.getCreatedAt()).isCloseTo(beforeSave, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldSetUpdatedTimestampOnPersist() {
        // Given: New auditable entity
        ZonedDateTime beforeSave = ZonedDateTime.now(ZoneOffset.UTC);
        assertThat(testEntity.getUpdatedAt()).isNull();

        // When: Entity is persisted
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: Updated timestamp is set
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getUpdatedAt().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldUpdateModificationTimestampOnUpdate() throws InterruptedException {
        // Given: Existing persisted entity
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);
        ZonedDateTime initialUpdatedAt = saved.getUpdatedAt();
        entityManager.clear();

        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // When: Entity is updated
        TestAuditableEntity found = entityManager.find(TestAuditableEntity.class, saved.getId());
        found.setTitle("Updated Title");
        TestAuditableEntity updated = entityManager.persistAndFlush(found);

        // Then: Updated timestamp is modified
        assertThat(updated.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    void shouldNotChangeCreationTimestampOnUpdate() {
        // Given: Existing persisted entity
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);
        ZonedDateTime creationTime = saved.getCreatedAt();
        entityManager.clear();

        // When: Entity is updated
        TestAuditableEntity found = entityManager.find(TestAuditableEntity.class, saved.getId());
        found.setTitle("Updated Title");
        TestAuditableEntity updated = entityManager.persistAndFlush(found);

        // Then: Creation timestamp remains unchanged
        assertThat(updated.getCreatedAt()).isEqualTo(creationTime);
    }

    @Test
    @WithMockUser(username = "test-user")
    void shouldCaptureCreatedByFromSecurityContext() {
        // Given: Authenticated user context
        assertThat(testEntity.getCreatedBy()).isNull();

        // When: Entity is persisted
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: Created by is captured from security context
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");
    }

    @Test
    @WithMockUser(username = "update-user")
    void shouldCaptureUpdatedByFromSecurityContext() {
        // Given: Existing entity created by different user
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);
        entityManager.clear();

        // When: Entity is updated by different user
        TestAuditableEntity found = entityManager.find(TestAuditableEntity.class, saved.getId());
        found.setTitle("Updated by different user");
        TestAuditableEntity updated = entityManager.persistAndFlush(found);

        // Then: Updated by reflects current user
        assertThat(updated.getUpdatedBy()).isEqualTo("update-user");
    }

    @Test
    void shouldDefaultIsDeletedToFalse() {
        // Given: New auditable entity
        assertThat(testEntity.getIsDeleted()).isNull();

        // When: Entity is persisted
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: isDeleted defaults to false
        assertThat(saved.getIsDeleted()).isFalse();
    }

    @Test
    void shouldFilterSoftDeletedEntitiesInQueries() {
        // Given: Active and soft-deleted entities
        TestAuditableEntity activeEntity = new TestAuditableEntity();
        activeEntity.setTitle("Active Entity");
        TestAuditableEntity saved1 = entityManager.persistAndFlush(activeEntity);

        TestAuditableEntity deletedEntity = new TestAuditableEntity();
        deletedEntity.setTitle("Deleted Entity");
        deletedEntity.setIsDeleted(true);
        TestAuditableEntity saved2 = entityManager.persistAndFlush(deletedEntity);

        entityManager.clear();

        // When: Querying all entities using repository methods
        List<TestAuditableEntity> entities = entityManager.getEntityManager()
            .createQuery("FROM TestAuditableEntity", TestAuditableEntity.class)
            .getResultList();

        // Then: Only active entities are returned due to @Where clause
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getId()).isEqualTo(saved1.getId());
        assertThat(entities.get(0).getIsDeleted()).isFalse();
    }

    @Test
    void shouldFindSoftDeletedEntitiesWithExplicitQuery() {
        // Given: Soft-deleted entity
        testEntity.setIsDeleted(true);
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);
        entityManager.clear();

        // When: Explicitly querying including deleted entities
        TestAuditableEntity found = entityManager.getEntityManager()
            .createQuery("FROM TestAuditableEntity e WHERE e.id = :id", TestAuditableEntity.class)
            .setParameter("id", saved.getId())
            .getSingleResult();

        // Then: Soft-deleted entity is found when explicitly queried
        assertThat(found).isNotNull();
        assertThat(found.getIsDeleted()).isTrue();
    }

    @Test
    void shouldInheritBaseEntityFunctionality() {
        // Given: AuditableEntity should extend BaseEntity
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: Should have BaseEntity fields (id, version)
        assertThat(saved.getId()).isNotNull(); // UUID from BaseEntity
        assertThat(saved.getVersion()).isEqualTo(0L); // Version from BaseEntity
    }

    @Test
    void shouldHandleTimezoneProperly() {
        // Given: Entity created in different timezone context
        TestAuditableEntity saved = entityManager.persistAndFlush(testEntity);

        // Then: All timestamps should be in UTC
        assertThat(saved.getCreatedAt().getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(saved.getUpdatedAt().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    /**
     * Test entity that extends AuditableEntity for testing purposes.
     * This will reference the AuditableEntity class that doesn't exist yet.
     */
    @Entity
    @Table(name = "test_auditable_entities")
    public static class TestAuditableEntity extends AuditableEntity {

        private String title;

        public TestAuditableEntity() {
            super();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}