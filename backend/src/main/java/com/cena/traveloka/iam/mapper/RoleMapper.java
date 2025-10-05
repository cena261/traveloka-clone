package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.RoleDto;
import com.cena.traveloka.iam.entity.Role;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleMapper {

    RoleDto toDto(Role role);

    List<RoleDto> toDtoList(List<Role> roles);

    List<RoleDto> setToDtoList(Set<Role> roles);
}
