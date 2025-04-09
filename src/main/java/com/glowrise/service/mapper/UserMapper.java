package com.glowrise.service.mapper;


import com.glowrise.domain.User;
import com.glowrise.service.dto.UserDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends EntityMapper<UserDTO, User> {
    @Named("withoutPassword")
    @Mapping(target = "password", ignore = true)
    UserDTO toDtoWithoutPassword(User user);

    @Override
    @IterableMapping(qualifiedByName = "withoutPassword")
    List<UserDTO> toDto(List<User> entityList);
}
