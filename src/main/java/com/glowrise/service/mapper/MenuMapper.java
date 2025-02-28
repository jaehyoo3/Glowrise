package com.glowrise.service.mapper;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Menu;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.dto.MenuDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper extends EntityMapper<MenuDTO, Menu> {
}
