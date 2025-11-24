package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CampoMontoDto implements Serializable {

    private Long monto;
    private DivisaDto divisa;
    private int tipoCambio;
}
