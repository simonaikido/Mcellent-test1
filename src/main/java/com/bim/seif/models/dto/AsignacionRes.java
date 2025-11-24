package com.bim.seif.models.dto;

import com.bim.seif.models.EstadoAsignacion;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class AsignacionRes {
    Long id;
    String titularEmail;
    String sustitutoEmail;
    OffsetDateTime fechaInicio;
    OffsetDateTime fechaFin;
    EstadoAsignacion estado;
    boolean autoRevert;
    boolean desactivadoTitular;
    String creadoPor;
    String motivo;
    OffsetDateTime createdAt;
}