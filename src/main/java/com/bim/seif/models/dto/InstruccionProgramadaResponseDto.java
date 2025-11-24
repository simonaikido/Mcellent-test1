package com.bim.seif.models.dto;

import com.bim.seif.models.Programacion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InstruccionProgramadaResponseDto {
    private String folio;
    private LocalDateTime fechaHoraAlta;
    private String aliasFideicomiso;
    private String regionFideicomiso;
    private String rutaArchivo;
    private String comentario;
    private String tipoInstruccion;
    private Boolean programada;
    private String monto;
    private String concepto;
    private String cuentaCargo;
    private String cuentaAbono;
    private String tipoOperacion;
    private List<Programacion> fechas;
}
