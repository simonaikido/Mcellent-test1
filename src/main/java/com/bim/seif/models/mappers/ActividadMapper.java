package com.bim.seif.models.mappers;

import com.bim.seif.models.Actividad;
import com.bim.seif.models.dto.ActividadDto;

import java.util.List;
import java.util.stream.Collectors;

public class ActividadMapper {

    public static ActividadDto toDto(Actividad actividad) {
        if (actividad == null) {
            return null;
        }
        ActividadDto dto = new ActividadDto();
        dto.setId(actividad.getId());
        dto.setFechaHora(actividad.getFechaHora());
        dto.setDescripcion(actividad.getDescripcion());
        dto.setUsuario(actividad.getUsuario());
        // Aquí asignamos solo el folio de la instrucción
        if (actividad.getInstruccion() != null) {
            dto.setFolioInstruccion(actividad.getInstruccion().getFolio());
        }
        return dto;
    }

    public static List<ActividadDto> toDto(List<Actividad> actividades) {
        if (actividades == null) {
            return null;
        }
        return actividades.stream()
                .map(ActividadMapper::toDto)
                .collect(Collectors.toList());
    }
}