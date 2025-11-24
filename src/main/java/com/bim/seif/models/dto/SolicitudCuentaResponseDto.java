package com.bim.seif.models.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitudCuentaResponseDto {
    private Long id;
    private Long cuenta;
    private String banco;
    private String divisaCve; 
    private String beneficiario;
    private String rfc;
    private String direccion;
    private String rutaEdoCta;
    private String rutaInstruccion;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaCarga;
    private LocalDateTime fechaFinalizacion;
    private String folioInstruccion;     // solo el folio
    private String folioFideicomiso;     // solo el folio
}