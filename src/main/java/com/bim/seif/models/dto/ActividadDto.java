package com.bim.seif.models.dto;

import lombok.Data; // Para getters, setters, toString, equals, hashCode
import java.time.LocalDateTime;

@Data
public class ActividadDto {
    private Long id;
    private String folioInstruccion; // Solo el folio de la instrucción
    private LocalDateTime fechaHora;
    private String descripcion;
    private String usuario;
}