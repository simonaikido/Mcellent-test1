package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="instruccion_folio")
    private InstruccionMonetaria instruccion; // Para vincular con la instrucción
    private LocalDateTime fechaHora;
    private String descripcion; // Ej: "Registro de operación", "Instrucción creada"
    private String usuario;

}