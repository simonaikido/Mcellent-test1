package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class InstruccionProgramadaDto implements Serializable {

    private String folio;
    private String comentario;
    private String concepto;
    private Long cuentaCargo;
    private Long cuentaAbonoCuenta;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaClasificacion;
    private int monto;
    private String referencia;
    private String responsableEmail;
    private String divisaCve;
    private String tipoOperacionMonetariaCve;
    private List<Date> fechas;
}
