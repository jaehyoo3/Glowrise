package com.glowrise.service.mapper;

import com.glowrise.domain.Advertisement;
import com.glowrise.service.dto.AdvertisementDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdvertisementMapper extends EntityMapper<AdvertisementDTO, Advertisement> {
}