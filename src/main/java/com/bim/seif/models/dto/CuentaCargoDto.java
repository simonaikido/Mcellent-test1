package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CuentaCargoDto implements Serializable {
    private final String cuenta;
    private final EntidadFinancieraDto entidadFinanciera;
    private final DivisaDto divisa;
    private final FideicomisoDto fideicomiso;
}
