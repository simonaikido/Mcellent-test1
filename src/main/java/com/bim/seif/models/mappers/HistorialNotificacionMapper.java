package com.bim.seif.models.mappers;

import com.bim.seif.models.HistorialNotificacion;
import com.bim.seif.models.dto.HistorialNotificacionDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface HistorialNotificacionMapper {
    HistorialNotificacion toEntity(HistorialNotificacionDto historialNotificacionDto);

    @Named("simple")
    HistorialNotificacionDto toDto(HistorialNotificacion historialNotificacion);

    @IterableMapping(qualifiedByName = "simple")
    List<HistorialNotificacionDto> toDto(List<HistorialNotificacion> historialNotificacion);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    HistorialNotificacion partialUpdate(HistorialNotificacionDto historialNotificacionDto, @MappingTarget HistorialNotificacion historialNotificacion);
}