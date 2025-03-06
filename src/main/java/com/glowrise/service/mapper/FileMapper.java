package com.glowrise.service.mapper;

import com.glowrise.domain.Files;
import com.glowrise.service.dto.FileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface FileMapper extends EntityMapper<FileDTO, Files> {
}
