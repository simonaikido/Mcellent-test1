package com.bim.seif.models.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventoAuditoriaDto {

    private long id;
    private String cve;
    private String asunto;
    private String comentario;
    private String cuerpoCorreo;
    private String rutaFirma;
    private boolean desactivado;
    private LocalDateTime fechaModificacion;
    private String modificadoPor;
}
