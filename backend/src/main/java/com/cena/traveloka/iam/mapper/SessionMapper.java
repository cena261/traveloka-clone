package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.entity.Session;
import org.mapstruct.*;

/**
 * T044: SessionMapper
 * MapStruct mapper for Session entity ↔ SessionDto conversion.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - Mapper separates entities from DTOs
 * - Principle X: Code Quality - MapStruct for type-safe mapping
 * - Used by SessionService for session management (FR-013, FR-016)
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SessionMapper {

    /**
     * Convert Session entity to SessionDto.
     * Excludes sensitive tokens (sessionToken, refreshToken).
     * Used in GET /api/v1/users/me/sessions.
     *
     * @param session Session entity
     * @return SessionDto without sensitive data
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "isCurrent", ignore = true)
    SessionDto toDto(Session session);

    /**
     * Convert Session entity to SessionDto with current session indicator.
     * Marks if this session is the current active session.
     *
     * @param session Session entity
     * @param isCurrent Whether this is the current session
     * @return SessionDto with isCurrent flag
     */
    @Mapping(target = "userId", source = "session.user.id")
    @Mapping(target = "isCurrent", source = "isCurrent")
    SessionDto toDtoWithCurrentFlag(Session session, boolean isCurrent);
}
