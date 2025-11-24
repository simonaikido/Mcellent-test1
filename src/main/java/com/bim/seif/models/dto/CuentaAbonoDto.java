package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CuentaAbonoDto implements Serializable {
    private Long cuenta;
    private String banco;
    private DivisaDto divisa;
    private String beneficiario;
    private String direccion;
    private String rfc;
    private Boolean solicitudCuenta;
    private FideicomisoDto fideicomiso;
    private LocalDate fechaAlta;
    private LocalDateTime fechaBaja;
    private String regimen;
    private String rutaArchivo;
}
