package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DivisaDto implements Serializable {
    private String cve;
    private String abreviatura;
    private String descripcion;
    private int tipoCambioMonedaNacional;
    private boolean monedaNacional;
}
