package com.bim.seif.models.mappers;

import com.bim.seif.models.Evento;
import com.bim.seif.models.dto.EventoDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventoMapper {
    Evento toEntity(EventoDto eventoDto);
    EventoDto toDto(Evento evento);
    List<EventoDto> toDto(List<Evento> eventos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Evento partialUpdate(EventoDto eventoDto, @MappingTarget Evento evento);
}
