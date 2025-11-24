package com.bim.seif.models.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class TipoOperacionMonetariaDto implements Serializable {
    private final String cve;
    private final String descripcion;
    private final boolean validacionGeneral;
    private final boolean validacionMontos;
    private final boolean validacionMesaControl;
}
