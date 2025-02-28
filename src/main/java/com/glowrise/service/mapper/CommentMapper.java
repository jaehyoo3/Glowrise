package com.glowrise.service.mapper;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Comment;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.dto.CommentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper extends EntityMapper<CommentDTO, Comment> {
}
