package com.bim.seif.models.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NotificacionDto {

    private String cve;
    private String nombre;
    private String titulo;
    private String mensaje;
    private String modificadoPor;
    private String comentario;
    private boolean desactivada;
    private LocalDateTime fechaModificacion;
    private List<HistorialNotificacionDto> modificaciones;
}
