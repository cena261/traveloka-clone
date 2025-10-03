package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.RoleDto;
import com.cena.traveloka.iam.entity.Role;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

/**
 * T045: RoleMapper
 * MapStruct mapper for Role entity ↔ RoleDto conversion.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - Mapper separates entities from DTOs
 * - Principle X: Code Quality - MapStruct for type-safe mapping
 * - Used by UserMapper for nested role mapping (FR-005: RBAC)
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleMapper {

    /**
     * Convert Role entity to RoleDto.
     * Used in UserDetailDto for role information.
     *
     * @param role Role entity
     * @return RoleDto with role information
     */
    RoleDto toDto(Role role);

    /**
     * Convert list of Role entities to list of RoleDtos.
     * Used for collections in UserDetailDto.
     *
     * @param roles List of Role entities
     * @return List of RoleDtos
     */
    List<RoleDto> toDtoList(List<Role> roles);

    /**
     * Convert set of Role entities to list of RoleDtos.
     * User.roles is a Set, UserDetailDto.roles is a List.
     *
     * @param roles Set of Role entities
     * @return List of RoleDtos
     */
    List<RoleDto> setToDtoList(Set<Role> roles);
}
