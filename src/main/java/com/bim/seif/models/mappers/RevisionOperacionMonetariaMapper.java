package com.bim.seif.models.mappers;

import com.bim.seif.models.RevisionOperacionMonetaria;
import com.bim.seif.models.dto.RevisionOperacionMonetariaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface RevisionOperacionMonetariaMapper {
    RevisionOperacionMonetariaMapper INSTANCE = Mappers.getMapper(RevisionOperacionMonetariaMapper.class);
    List<RevisionOperacionMonetariaDto> toDto(List<RevisionOperacionMonetaria> operacionMonetaria);
    RevisionOperacionMonetaria toEntity(RevisionOperacionMonetariaDto revisionOperacionMonetariaDto);
    List<RevisionOperacionMonetaria> toEntity(List<RevisionOperacionMonetariaDto> revisionOperacionMonetariaDto);
}