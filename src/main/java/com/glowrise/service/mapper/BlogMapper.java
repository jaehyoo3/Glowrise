package com.glowrise.service.mapper;

import com.glowrise.domain.Blog;
import com.glowrise.service.dto.BlogDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlogMapper extends EntityMapper<BlogDTO, Blog> {
}
