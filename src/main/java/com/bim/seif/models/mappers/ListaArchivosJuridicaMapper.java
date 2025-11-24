package com.bim.seif.models.mappers;


import com.bim.seif.models.ListaArchivosJuridica;
import com.bim.seif.models.dto.ListaArchivosJuridicaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ListaArchivosJuridicaMapper {
    ListaArchivosJuridicaMapper INSTANCE = Mappers.getMapper(ListaArchivosJuridicaMapper.class);
    ListaArchivosJuridicaDto toDto(ListaArchivosJuridica tipo);
    List<ListaArchivosJuridicaDto> toDto(List<ListaArchivosJuridica> tipos);
}
