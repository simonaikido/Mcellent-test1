package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class DocumentoFideicomiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ruta;
    private String tipoArchivo;

    private LocalDateTime fechaCarga;

    private String fideicomisoFolio;
}
