package com.bim.seif.models.mappers;

import com.bim.seif.models.ConfiguracionOperacionMonetaria;
import com.bim.seif.models.dto.ConfiguracionOperacionMonetariaDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ConfiguracionOperacionMonetariaMapper {
    ConfiguracionOperacionMonetariaMapper INSTANCE = Mappers.getMapper(ConfiguracionOperacionMonetariaMapper.class);
    ConfiguracionOperacionMonetariaDto toDto(ConfiguracionOperacionMonetaria cfg);
    List<ConfiguracionOperacionMonetariaDto> toDto(List<ConfiguracionOperacionMonetaria> confs);
}
