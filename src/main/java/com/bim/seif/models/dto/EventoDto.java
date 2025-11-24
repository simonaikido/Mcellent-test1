package com.bim.seif.models.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventoDto {

    private String cve;
    private String asunto;
    private String cuerpoCorreo;
    private String rutaFirma;
    private String modificadoPor;
    private String comentario;
    private boolean desactivado;
    private LocalDateTime fechaModificacion;
    private String nombre;
    List<EventoAuditoriaDto> modificaciones;
}
