package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class TipoOperacionMonetariaProgramadaDto {
    private String cve;
    private String descripcion;

    public TipoOperacionMonetariaProgramadaDto(String cve, String descripcion) {
        this.cve = cve;
        this.descripcion = descripcion;
    }
    //Regresa el tipo de operacion para programadas segun modelo
}
