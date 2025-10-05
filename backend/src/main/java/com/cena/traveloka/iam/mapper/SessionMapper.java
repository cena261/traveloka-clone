package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.entity.Session;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SessionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "isCurrent", ignore = true)
    SessionDto toDto(Session session);

    @Mapping(target = "userId", source = "session.user.id")
    @Mapping(target = "isCurrent", source = "isCurrent")
    SessionDto toDtoWithCurrentFlag(Session session, boolean isCurrent);
}
