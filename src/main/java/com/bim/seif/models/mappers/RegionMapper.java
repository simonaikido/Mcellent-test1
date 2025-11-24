package com.bim.seif.models.mappers;

import com.bim.seif.models.Region;
import com.bim.seif.models.dto.RegionDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegionMapper {

    RegionMapper INSTANCE = Mappers.getMapper(RegionMapper.class);

    Region toEntity(RegionDto regionDto);

    RegionDto toDto(Region region);

    List<RegionDto> toDto(List<Region> region);
//aalcalar@mcllent.com
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Region partialUpdate(RegionDto regionDto, @MappingTarget Region region);
}