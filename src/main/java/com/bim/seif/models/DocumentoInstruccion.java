package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore; // Importa JsonIgnore

import java.time.LocalDateTime;

@Data
@Entity
public class DocumentoInstruccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ruta;

    private LocalDateTime fechaCarga;

    @ManyToOne
    @JoinColumn(name = "instruccion_folio")
    @JsonIgnore // <-- Añade esta anotación aquí
    private Instruccion instruccion;
}