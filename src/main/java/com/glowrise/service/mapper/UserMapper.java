package com.glowrise.service.mapper;

import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends EntityMapper<UserDTO, User> {
}
