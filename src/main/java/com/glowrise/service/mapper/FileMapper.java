package com.glowrise.service.mapper;

import com.glowrise.domain.Files;
import com.glowrise.domain.Post;
import com.glowrise.service.dto.FileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileMapper extends EntityMapper<FileDTO, Files> {
    @Named("mapPostIdToPostEntity")
    default Post mapPostIdToPostEntity(Long postId) {
        if (postId == null) {
            return null;
        }
        Post post = new Post();
        post.setId(postId);
        return post;
    }
}
