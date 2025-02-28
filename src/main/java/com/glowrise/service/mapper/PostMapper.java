package com.glowrise.service.mapper;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Post;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper extends EntityMapper<PostDTO, Post> {
}
