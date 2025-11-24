package com.bim.seif.models.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolicitudCorreccionDto {
    private long id;
    private String comentarios;
    private LocalDateTime fechaRevision;
    private LocalDateTime fechaCorreccion;
    private String validadorEmail;
}
