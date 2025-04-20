package com.glowrise.service.mapper;

import com.glowrise.domain.Advertisement;
import com.glowrise.domain.StoredFile;
import com.glowrise.service.dto.AdvertisementDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AdvertisementMapper extends EntityMapper<AdvertisementDTO, Advertisement> {

    @Named("fileToUrl")
    default String fileToUrl(StoredFile file) {
        if (file == null || file.getId() == null) {
            return null;
        }
        return "/api/files/" + file.getId();
    }
}