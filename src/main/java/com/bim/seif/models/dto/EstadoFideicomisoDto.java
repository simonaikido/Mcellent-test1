package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EstadoFideicomisoDto implements Serializable {
    private final String cve;
    private final String descripcion;
    private final String tipo;
}
