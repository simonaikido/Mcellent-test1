package com.bim.seif.models.mappers;

import com.bim.seif.models.TipoOperacionMonetaria;
import com.bim.seif.models.dto.TipoOperacionMonetariaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TipoOperacionMonetariaMapper {
    TipoOperacionMonetariaMapper INSTANCE = Mappers.getMapper(TipoOperacionMonetariaMapper.class);
    TipoOperacionMonetariaDto toDto(TipoOperacionMonetaria tipo);
    List<TipoOperacionMonetariaDto> toDto(List<TipoOperacionMonetaria> tipos);
}
