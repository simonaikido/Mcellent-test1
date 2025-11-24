package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class SolicitudDocumentoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ruta;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaCarga;

    @ManyToOne
    @JoinColumn(name = "instruccion_folio")
    private Instruccion instruccion;
}
