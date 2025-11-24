package com.bim.seif.models.mappers;

import com.bim.seif.models.OperacionJuridica;
import com.bim.seif.models.dto.OperacionJuridicaDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface OperacionJuridicaMapper {

    // ——— MAPEOS NORMALES ———
    @Mapping(target = "instruccion", ignore = true) // << rompe back-reference en DTO
    @Mapping(source = "estatus.cve", target = "estatusCve") // <--- ¡Esta es la solución!
    OperacionJuridicaDto toDto(OperacionJuridica entity);

    List<OperacionJuridicaDto> toDto(List<OperacionJuridica> entities);

    OperacionJuridica toEntity(OperacionJuridicaDto dto);

    List<OperacionJuridica> toEntity(List<OperacionJuridicaDto> dtos);

    // ——— VARIANTES SHALLOW PARA USAR DESDE INSTRUCCION ———
    @Named("toDtoShallow")
    @Mapping(target = "instruccion", ignore = true) // << imprescindible
    OperacionJuridicaDto toDtoShallow(OperacionJuridica entity);

    @Named("toDtoShallowList")
    @IterableMapping(qualifiedByName = "toDtoShallow")
    List<OperacionJuridicaDto> toDtoShallowList(List<OperacionJuridica> entities);
}