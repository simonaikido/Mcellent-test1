package com.bim.seif.models.dto;

import com.bim.seif.models.TipoInstruccion;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InstruccionDto implements Serializable {
    private String folio;
    private FideicomisoDto fideicomiso;
    private TipoInstruccion tipo;
    private String rutaArchivo;
    private String comentario;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaAtencion;
    private String clienteCarga;
    private LocalDateTime fechaRechazo;
}
