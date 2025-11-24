package com.bim.seif.models.mappers;

import com.bim.seif.models.Fideicomiso;
import com.bim.seif.models.dto.FideicomisoDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FideicomisoMapper {
    FideicomisoDto toDto(Fideicomiso fideicomiso);
}
