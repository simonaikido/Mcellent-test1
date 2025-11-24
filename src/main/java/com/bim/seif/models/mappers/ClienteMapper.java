package com.bim.seif.models.mappers;

import com.bim.seif.models.Cliente;
import com.bim.seif.models.dto.ClienteDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClienteMapper {
    ClienteMapper INSTANCE = Mappers.getMapper(ClienteMapper.class);
    ClienteDto toDto(Cliente cliente);
    List<ClienteDto> toDto(List<Cliente> cliente);
    Cliente toEntity(ClienteDto dto);
}
