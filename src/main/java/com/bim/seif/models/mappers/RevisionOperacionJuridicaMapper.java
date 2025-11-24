package com.bim.seif.models.mappers;

import com.bim.seif.models.RevisionOperacionJuridica;
import com.bim.seif.models.dto.RevisionOperacionJuridicaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RevisionOperacionJuridicaMapper {

    RevisionOperacionJuridicaMapper INSTANCE = Mappers.getMapper(RevisionOperacionJuridicaMapper.class);

    RevisionOperacionJuridicaDto toDto(RevisionOperacionJuridica entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "operacionJuridica", ignore = true)
    @Mapping(target = "fechaRevision", ignore = true)
    RevisionOperacionJuridica toEntity(RevisionOperacionJuridicaDto dto);
}