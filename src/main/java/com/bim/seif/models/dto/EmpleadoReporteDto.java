package com.bim.seif.models.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO plano para mostrar los registros crudos de auditoría de empleados.
 * Incluye toda la información de la tabla empleado_audit sin el ID.
 */
@Data
public class EmpleadoReporteDto implements Serializable {

    private String uid;          // identificador único del usuario
    private String accion;       // acción registrada en la auditoría
    private String actor;        // usuario que ejecutó la acción
    private String ip;           // IP origen
    private String userAgent;    // agente del navegador
    private String payloadBefore; // JSON antes del cambio
    private String payloadAfter;  // JSON después del cambio
    private OffsetDateTime createdAt; // fecha/hora de la auditoría
    private String comentario;    // comentario de auditoría
}