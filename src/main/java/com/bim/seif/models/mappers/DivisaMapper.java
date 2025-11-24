package com.bim.seif.models.mappers;

import com.bim.seif.models.Divisa;
import com.bim.seif.models.dto.DivisaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DivisaMapper {
    DivisaMapper INSTANCE = Mappers.getMapper(DivisaMapper.class);
    DivisaDto toDto(Divisa divisa);
}
