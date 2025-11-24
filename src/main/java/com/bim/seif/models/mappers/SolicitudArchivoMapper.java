package com.bim.seif.models.mappers;

import com.bim.seif.models.SolicitudArchivoJuridica;
import com.bim.seif.models.dto.SolicitudArchivoJuridicaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SolicitudArchivoMapper {
    SolicitudArchivoMapper INSTANCE = Mappers.getMapper(SolicitudArchivoMapper.class);

    List<SolicitudArchivoJuridicaDto> toDtoList(List<SolicitudArchivoJuridica> solicitudArchivos);

}
