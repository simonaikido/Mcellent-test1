package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InstruccionMonetariaResponseDto implements Serializable {
    private String folio;
    private InstruccionDto instruccion;
    private String responsable;
    private String validador_email;
    private boolean operadaParcialmente;
    private String estatus;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaClasificacion;
    private boolean  prioritaria;
    private String observaciones;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaAprobacion;
    private boolean solicitudCorreccion;
    private boolean programda;
    private LocalDateTime fechaRechazo;
}
