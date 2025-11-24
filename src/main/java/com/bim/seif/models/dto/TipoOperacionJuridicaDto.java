package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TipoOperacionJuridicaDto implements Serializable {
    private final String cve;
    private final String descripcion;
}
