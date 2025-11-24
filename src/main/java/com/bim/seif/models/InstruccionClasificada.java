package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;


public class InstruccionClasificada {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String folio;
    private String nombreFideicomiso;
    private String aliasFideicomiso;
    private String regionFideicomiso;
    private LocalDateTime fechaHoraRegistro;
    private String archivoInstruccionPdfUrl;
    private String tipoInstruccion; // "Monetaria", "No Monetaria", "Ambas"
    private String estatus; // "Proceso", "Pendiente", "Cancelada", etc.



}