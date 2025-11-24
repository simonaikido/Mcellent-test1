package com.bim.seif.models.mappers;

import com.bim.seif.models.TipoOperacionJuridica;
import com.bim.seif.models.dto.TipoOperacionJuridicaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TipoOperacionJuridicaMapper {
    TipoOperacionJuridicaMapper INSTANCE = Mappers.getMapper(TipoOperacionJuridicaMapper.class);
    TipoOperacionJuridicaDto toDto(TipoOperacionJuridica tipo);
    List<TipoOperacionJuridicaDto> toDto(List<TipoOperacionJuridica> tipos);
}
