package com.bim.seif.models.mappers;

import com.bim.seif.models.Notificacion;
import com.bim.seif.models.dto.NotificacionDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificacionMapper {
    Notificacion toEntity(NotificacionDto notificacionDto);

    NotificacionDto toDto(Notificacion notificacion);

    List<NotificacionDto> toDto(List<Notificacion> notificaciones);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Notificacion partialUpdate(NotificacionDto notificacionDto, @MappingTarget Notificacion notificacion);
}